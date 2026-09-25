package com.sms.movies.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sms.movies.dto.MovieRequest;
import com.sms.movies.dto.DiscoveryDtos.*;
import com.sms.movies.exception.*;
import com.sms.movies.model.Movie;
import com.sms.movies.repository.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;

class CatalogServiceCoverageTest {
  private static final UUID MOVIE = UUID.randomUUID();
  private static final UUID USER = UUID.randomUUID();

  @Test
  void detailsPeopleAndReferenceCreation() throws Exception {
    var sql = mock(SqlRepository.class);
    var movies = mock(MovieRepository.class);
    var service = new CatalogService(sql, movies, mock(ActivityReporter.class));
    var movie = new Movie(MOVIE, "Film", "Description", "http://poster", "Director", "Drama",
        LocalDate.of(2020, 1, 1), 100);
    when(movies.findById(MOVIE)).thenReturn(Optional.of(movie));
    when(sql.one(contains("personal_rating"), any(), any(Object[].class))).thenReturn(4);
    when(sql.list(anyString(), any(), any(Object[].class))).thenReturn(List.of());
    when(sql.count(anyString(), any(Object[].class))).thenReturn(0L);

    assertEquals(4, service.details(MOVIE, USER).personalRating());
    assertNull(service.details(MOVIE, null).personalRating());
    assertTrue(service.genres().isEmpty());
    assertEquals(0, service.people("a%_!", "CAST", 0, 20).totalElements());
    assertThrows(ApiException.class, () -> service.people("", "WRITER", 0, 20));
    assertEquals("Actor", service.createPerson(" Actor ").name());
    assertEquals("Drama", service.createGenre(" Drama ").name());
    assertTrue(service.credits(MOVIE).isEmpty());
    assertTrue(service.movieGenres(MOVIE).isEmpty());

    when(movies.findById(any())).thenReturn(Optional.empty());
    assertThrows(MovieNotFoundException.class, () -> service.details(UUID.randomUUID(), USER));
  }

  @Test
  void syncCreatesMissingLegacyReferencesAndDeduplicatesIds() {
    var sql = mock(SqlRepository.class);
    var service = new CatalogService(sql, mock(MovieRepository.class), mock(ActivityReporter.class));
    var director = UUID.randomUUID();
    var genre = UUID.randomUUID();
    when(sql.one(contains("FROM person"), any(), any(Object[].class))).thenReturn(null);
    when(sql.one(contains("FROM genre"), any(), any(Object[].class))).thenReturn(null);
    var request = new MovieRequest("Film", null, null, "Director", "Drama", LocalDate.now(), 90,
        List.of(director, director), List.of(director), List.of(genre, genre));

    service.sync(MOVIE, request);

    verify(sql, atLeastOnce()).update(contains("DELETE FROM movie_credit"), eq(MOVIE));
    verify(sql, atLeastOnce()).update(contains("INSERT INTO movie_credit"), any(Object[].class));
    verify(sql, atLeastOnce()).update(contains("INSERT INTO movie_genre"), any(Object[].class));
  }

  @Test
  void externalRatingValidatesUpsertsAndReturnsStoredValue() throws Exception {
    var sql = mock(SqlRepository.class);
    var activity = mock(ActivityReporter.class);
    var service = new CatalogService(sql, mock(MovieRepository.class), activity);
    var request = new ExternalRatingRequest(8.0, 10.0, 100L);
    when(sql.one(contains("SELECT id FROM movies"), any(), any(Object[].class))).thenReturn(MOVIE);
    when(sql.update(startsWith("UPDATE external_rating"), any(Object[].class))).thenReturn(0);
    when(sql.list(contains("external_rating"), any(), any(Object[].class))).thenAnswer(invocation -> {
      var row = mock(java.sql.ResultSet.class);
      when(row.getString("source")).thenReturn("IMDb");
      when(row.getDouble("score")).thenReturn(8.0);
      when(row.getDouble("scale_max")).thenReturn(10.0);
      when(row.getObject("vote_count")).thenReturn(100L);
      when(row.getTimestamp("fetched_at")).thenReturn(Timestamp.from(Instant.now()));
      return List.of(invocation.<RowMapper<ExternalRating>>getArgument(1).mapRow(row, 0));
    });

    assertEquals("IMDb", service.putExternal(USER, MOVIE, "IMDb", request).source());
    verify(sql).update(startsWith("INSERT INTO external_rating"), any(Object[].class));
    verify(activity).audit(USER, "EXTERNAL_RATING_UPDATED", "MOVIE", MOVIE, "SUCCESS");

    assertThrows(ApiException.class,
        () -> service.putExternal(USER, MOVIE, "IMDb", new ExternalRatingRequest(11.0, 10.0, 1L)));
    when(sql.one(contains("SELECT id FROM movies"), any(), any(Object[].class))).thenReturn(null);
    assertThrows(MovieNotFoundException.class, () -> service.putExternal(USER, MOVIE, "IMDb", request));
  }
}
