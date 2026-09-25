package com.sms.movies.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sms.movies.dto.AccountDtos.*;
import com.sms.movies.dto.CollectionDtos.*;
import com.sms.movies.dto.MovieRequest;
import com.sms.movies.dto.MovieResponse;
import com.sms.movies.dto.PageResponse;
import com.sms.movies.exception.ApiException;
import com.sms.movies.exception.MovieNotFoundException;
import com.sms.movies.model.Movie;
import com.sms.movies.repository.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

class CoreServicesTest {
  private static final UUID USER = UUID.randomUUID();
  private static final UUID MOVIE = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  @Test
  void accountRegistrationNormalizesAndCreatesDefaults() {
    var users = mock(UserRepository.class);
    var sql = mock(SqlRepository.class);
    var passwords = mock(PasswordEncoder.class);
    var activity = mock(ActivityReporter.class);
    var expected = new UserResponse(USER, "a@example.com", "Alice", NOW);
    when(passwords.encode("long-password")).thenReturn("hash");
    when(users.create(eq("a@example.com"), eq("hash"), eq("Alice"))).thenReturn(USER);
    when(users.find("a@example.com")).thenReturn(new UserRepository.Account(expected, "hash", "USER"));

    var result = new AccountService(users, sql, passwords, activity)
        .register(new RegisterRequest(" A@Example.COM ", "long-password", " Alice "));

    assertEquals(expected, result);
    verify(sql).update(contains("user_collection"), any(UUID.class), eq(USER), eq("My movies"));
    verify(activity, times(2)).audit(eq(USER), anyString(), anyString(), any(), eq("SUCCESS"));
    assertEquals("a@example.com", AccountService.normalizeEmail(" A@Example.COM "));
  }

  @Test
  void analyticsRecordsNewEventsAndTreatsExactReplayAsIdempotent() {
    var sql = mock(SqlRepository.class);
    var users = mock(UserRepository.class);
    var service = new AnalyticsService(sql, users);
    var event = new com.sms.movies.dto.AnalyticsDtos.EventRequest(
        UUID.randomUUID(),
        com.sms.movies.dto.AnalyticsDtos.ClientEvent.RECOMMENDATION_VIEWED,
        MOVIE,
        UUID.randomUUID(),
        true);
    when(sql.count(anyString(), any(Object[].class))).thenReturn(1L);

    service.record(USER, "view", MOVIE);
    service.accept(USER, event);
    verify(sql, times(2)).update(anyString(), any(Object[].class));
    verify(users).lock(USER);

    var existing = mockExistingRow(USER, "recommendation_viewed", MOVIE, event.journeyId().toString());
    when(sql.one(anyString(), any(), any(Object[].class))).thenAnswer(existing);
    service.accept(USER, event);

    var conflict = mockExistingRow(UUID.randomUUID(), "recommendation_viewed", MOVIE, event.journeyId().toString());
    when(sql.one(anyString(), any(), any(Object[].class))).thenAnswer(conflict);
    assertEquals(409, assertThrows(ApiException.class, () -> service.accept(USER, event)).status().value());
  }

  private static org.mockito.stubbing.Answer<Object> mockExistingRow(
      UUID user, String name, UUID movie, String journey) {
    return invocation -> {
      var row = mock(java.sql.ResultSet.class);
      when(row.getObject("user_id", UUID.class)).thenReturn(user);
      when(row.getString("event_name")).thenReturn(name);
      when(row.getObject("movie_id", UUID.class)).thenReturn(movie);
      when(row.getString("journey_id")).thenReturn(journey);
      return invocation.<org.springframework.jdbc.core.RowMapper<Object>>getArgument(1).mapRow(row, 0);
    };
  }

  @Test
  void auditBuildsFiltersAndRecordsCorrelation() {
    var sql = mock(SqlRepository.class);
    when(sql.count(anyString(), any(Object[].class))).thenReturn(0L);
    when(sql.list(anyString(), any(), any(Object[].class))).thenReturn(List.of());
    var service = new AuditService(sql);
    org.slf4j.MDC.put("correlationId", "corr");
    try {
      assertEquals("corr", AuditService.correlationId());
      service.record(USER, "CREATE", "MOVIE", MOVIE, "SUCCESS");
      var result = service.list(USER, "CREATE", NOW.minusSeconds(5), NOW, 1, 10);
      assertEquals(0, result.totalElements());
      var query = ArgumentCaptor.forClass(String.class);
      verify(sql).count(query.capture(), any(Object[].class));
      assertTrue(query.getValue().contains("action=?"));
      assertTrue(query.getValue().contains("occurred_at>=?"));
      assertTrue(query.getValue().contains("occurred_at<=?"));
    } finally {
      org.slf4j.MDC.clear();
    }
    assertNotNull(AuditService.correlationId());
  }

  @Test
  void movieCrudMapsRequestsAndMissingMovies() {
    var repository = mock(MovieRepository.class);
    var catalog = mock(CatalogService.class);
    var service = new MovieService(repository, catalog);
    var request = new MovieRequest(
        " Film ", "Description", "http://poster", " Director ", " Drama ",
        LocalDate.of(2020, 1, 2), 120, List.of(), List.of(), List.of());
    var saved = new Movie(MOVIE, "Film", "Description", "poster", "Director", "Drama", request.releaseDate(), 120);
    when(repository.save(any(Movie.class))).thenReturn(saved);
    when(repository.findById(MOVIE)).thenReturn(Optional.of(saved));
    when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(saved)));

    assertEquals(1, service.list(0, 20).totalElements());
    assertEquals(MOVIE, service.get(MOVIE).id());
    assertEquals(MOVIE, service.create(request).id());
    assertEquals(MOVIE, service.update(MOVIE, request).id());
    service.delete(MOVIE);
    verify(catalog, times(2)).sync(MOVIE, request);
    verify(repository).delete(saved);

    when(repository.findById(any())).thenReturn(Optional.empty());
    assertThrows(MovieNotFoundException.class, () -> service.get(UUID.randomUUID()));
  }

  @Test
  void collectionsCoverOwnershipCrudAndMembership() {
    var sql = mock(SqlRepository.class);
    var movies = mock(MovieService.class);
    var activity = mock(ActivityReporter.class);
    var service = new CollectionService(sql, movies, activity);
    var collection = UUID.randomUUID();
    var response = new CollectionResponse(collection, "List", NOW, NOW);
    when(sql.one(anyString(), any(), any(Object[].class))).thenReturn(response);
    when(sql.list(anyString(), any(), any(Object[].class))).thenReturn(List.of());
    when(sql.count(anyString(), any(Object[].class))).thenReturn(0L);

    assertEquals(response, service.owned(USER, collection));
    assertEquals(0, service.list(USER, 0, 20).totalElements());
    assertNotNull(service.create(USER, " New "));
    assertNotNull(service.get(USER, collection, 0, 20));
    assertEquals(response, service.rename(USER, collection, " Renamed "));
    service.add(USER, collection, MOVIE);
    service.remove(USER, collection, MOVIE);
    service.delete(USER, collection);
    verify(movies).get(MOVIE);
    verify(activity, atLeast(3)).audit(eq(USER), anyString(), anyString(), any(), eq("SUCCESS"));

    when(sql.one(anyString(), any(), any(Object[].class))).thenReturn(null);
    assertThrows(ApiException.class, () -> service.owned(USER, collection));
  }

  @Test
  void sharingCoversCreateListRevokeAndReadValidation() {
    var sql = mock(SqlRepository.class);
    var collections = mock(CollectionService.class);
    var activity = mock(ActivityReporter.class);
    var tokens = mock(ShareTokenCodec.class);
    var service = new ShareService(sql, collections, activity, tokens);
    var collection = UUID.randomUUID();
    when(tokens.generate()).thenReturn("token");
    when(tokens.digest("token")).thenReturn("digest");
    when(tokens.isValid("token")).thenReturn(true);
    when(sql.count(anyString(), any(Object[].class))).thenReturn(1L);
    when(sql.update(anyString(), any(Object[].class))).thenReturn(1);
    when(sql.list(anyString(), any(), any(Object[].class))).thenReturn(List.of());

    assertEquals("token", service.create(USER, collection, NOW.plusSeconds(60)).token());
    assertTrue(service.list(USER, collection).isEmpty());
    service.revoke(USER, collection, UUID.randomUUID());
    verify(activity, atLeastOnce()).audit(eq(USER), anyString(), anyString(), any(), eq("SUCCESS"));

    when(sql.one(anyString(), any(), any(Object[].class))).thenAnswer(invocation -> Map.entry(collection, "Shared"));
    when(collections.items(collection, 0, 20)).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));
    assertEquals("Shared", service.read("token", 0, 20).name());
    when(tokens.isValid(anyString())).thenReturn(false);
    assertThrows(ApiException.class, () -> service.read("bad", 0, 20));
  }

  @Test
  void defaultReporterDelegates() {
    var audit = mock(AuditService.class);
    var analytics = mock(AnalyticsService.class);
    var reporter = new DefaultActivityReporter(audit, analytics);
    reporter.audit(USER, "A", "T", MOVIE, "SUCCESS");
    reporter.analytics(USER, "view", MOVIE);
    assertNotNull(reporter.correlationId());
    verify(audit).record(USER, "A", "T", MOVIE, "SUCCESS");
    verify(analytics).record(USER, "view", MOVIE);
  }
}
