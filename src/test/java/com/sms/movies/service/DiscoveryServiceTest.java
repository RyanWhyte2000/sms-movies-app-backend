package com.sms.movies.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sms.movies.model.Movie;
import com.sms.movies.repository.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

class DiscoveryServiceTest {
  private static Movie movie(UUID id, String title) {
    return new Movie(id, title, "space adventure", "http://poster", "Director", "Sci-Fi",
        LocalDate.of(2024, 1, 1), 120);
  }

  @Test
  void searchReturnsRankedMatchesAndReportsActivity() {
    var movies = mock(MovieRepository.class);
    var sql = mock(SqlRepository.class);
    var engine = mock(RecommendationEngine.class);
    var activity = mock(ActivityReporter.class);
    var service = new DiscoveryService(movies, sql, engine, activity);
    var user = UUID.randomUUID();
    var id = UUID.randomUUID();
    when(movies.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(movie(id, "Moon"))));
    when(engine.rank(anyString(), anyList(), anySet(), eq(5)))
        .thenReturn(List.of(new RecommendationEngine.Match(id, .9)));
    when(engine.name()).thenReturn("test-engine");
    when(activity.correlationId()).thenReturn("corr");

    var result = service.search(user, "space", 5);

    assertFalse(result.fallbackUsed());
    assertEquals(id, result.results().get(0).movie().id());
    assertEquals(.9, result.results().get(0).relevance());
    verify(activity).audit(user, "NATURAL_LANGUAGE_SEARCH", "DISCOVERY", null, "SUCCESS");
    verify(activity).analytics(user, "search_completed", null);
    verify(sql).update(contains("ai_request"), any(Object[].class));
  }

  @Test
  void recommendationsUseHistoryExcludeSeenMoviesAndFallback() {
    var movies = mock(MovieRepository.class);
    var sql = mock(SqlRepository.class);
    var engine = mock(RecommendationEngine.class);
    var activity = mock(ActivityReporter.class);
    var service = new DiscoveryService(movies, sql, engine, activity);
    var user = UUID.randomUUID();
    var liked = UUID.randomUUID();
    var disliked = UUID.randomUUID();
    var unseen = UUID.randomUUID();
    when(movies.findAll(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(movie(liked, "Liked"), movie(disliked, "Bad"), movie(unseen, "New"))));
    when(sql.list(contains("collection_item"), any(), any(Object[].class))).thenReturn(List.of(liked));
    when(sql.list(contains("personal_rating"), any(), any(Object[].class)))
        .thenReturn(List.of(Map.entry(disliked, 2), Map.entry(liked, 5)));
    when(engine.rank(anyString(), anyList(), anySet(), eq(2))).thenReturn(List.of());
    when(engine.name()).thenReturn("test-engine");
    when(activity.correlationId()).thenReturn("corr");

    var result = service.recommendations(user, "more", 2);

    assertTrue(result.fallbackUsed());
    assertEquals(List.of(unseen), result.results().stream().map(r -> r.movie().id()).toList());
    var query = org.mockito.ArgumentCaptor.forClass(String.class);
    @SuppressWarnings({"rawtypes", "unchecked"})
    org.mockito.ArgumentCaptor<Set<UUID>> excluded =
        org.mockito.ArgumentCaptor.forClass((Class) Set.class);
    verify(engine).rank(query.capture(), anyList(), excluded.capture(), eq(2));
    assertTrue(query.getValue().contains("Liked"));
    assertTrue(excluded.getValue().containsAll(Set.of(liked, disliked)));
    verify(activity).analytics(user, "recommendation_completed", null);
  }
}
