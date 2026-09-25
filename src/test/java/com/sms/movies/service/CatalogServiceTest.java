package com.sms.movies.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sms.movies.repository.MovieRepository;
import com.sms.movies.repository.SqlRepository;
import com.sms.movies.dto.DiscoveryDtos.ExternalRating;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;

class CatalogServiceTest {
  @Test
  void searchCombinesCastDirectorAndGenreFilters() {
    var sql = mock(SqlRepository.class);
    var movies = mock(MovieRepository.class);
    var activity = mock(ActivityReporter.class);
    var service = new CatalogService(sql, movies, activity);
    var cast = UUID.randomUUID();
    var director = UUID.randomUUID();
    var genre = UUID.randomUUID();

    when(sql.count(anyString(), any(Object[].class))).thenReturn(0L);
    when(sql.list(anyString(), any(), any(Object[].class))).thenReturn(List.of());

    service.search(UUID.randomUUID(), null, cast, director, genre, 0, 20);

    var query = ArgumentCaptor.forClass(String.class);
    verify(sql).count(query.capture(), any(Object[].class));
    assertTrue(query.getValue().contains("c.role='CAST'"));
    assertTrue(query.getValue().contains("c.role='DIRECTOR'"));
    assertTrue(query.getValue().contains("g.genre_id=?"));
  }

  @Test
  void guestCanSearchWithoutWritingUserAnalytics() {
    var sql = mock(SqlRepository.class);
    var activity = mock(ActivityReporter.class);
    var service = new CatalogService(sql, mock(MovieRepository.class), activity);
    when(sql.count(anyString(), any(Object[].class))).thenReturn(0L);
    when(sql.list(anyString(), any(), any(Object[].class))).thenReturn(List.of());

    var result = service.search(null, "comedy", null, null, null, 0, 20);

    assertEquals(0, result.totalElements());
    verifyNoInteractions(activity);
  }

  @Test
  void exposesThirdPartyRatingsForMovieDetails() throws Exception {
    var sql = mock(SqlRepository.class);
    var service = new CatalogService(sql, mock(MovieRepository.class), mock(ActivityReporter.class));
    var movie = UUID.randomUUID();
    var fetchedAt = Instant.parse("2026-01-02T03:04:05Z");
    var row = mock(java.sql.ResultSet.class);
    when(row.getString("source")).thenReturn("IMDb");
    when(row.getDouble("score")).thenReturn(8.3);
    when(row.getDouble("scale_max")).thenReturn(10.0);
    when(row.getObject("vote_count")).thenReturn(2500L);
    when(row.getTimestamp("fetched_at")).thenReturn(Timestamp.from(fetchedAt));
    when(sql.list(anyString(), org.mockito.ArgumentMatchers.<RowMapper<ExternalRating>>any(), any(Object[].class)))
        .thenAnswer(invocation -> List.of(invocation.<RowMapper<ExternalRating>>getArgument(1).mapRow(row, 0)));

    var ratings = service.externalRatings(movie);

    assertEquals(List.of(new ExternalRating("IMDb", 8.3, 10.0, 2500L, fetchedAt)), ratings);
  }
}
