package com.sms.movies.service;

import com.sms.movies.dto.DiscoveryDtos.*;
import com.sms.movies.dto.MovieResponse;
import com.sms.movies.model.Movie;
import com.sms.movies.repository.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscoveryService {
  private final MovieRepository movies;
  private final SqlRepository sql;
  private final ContentRecommendationEngine engine;
  private final AuditService audit;
  private final AnalyticsService analytics;

  public DiscoveryService(
      MovieRepository movies,
      SqlRepository sql,
      ContentRecommendationEngine engine,
      AuditService audit,
      AnalyticsService analytics) {
    this.movies = movies;
    this.sql = sql;
    this.engine = engine;
    this.audit = audit;
    this.analytics = analytics;
  }

  @Transactional
  public DiscoveryResponse search(long user, String query, int limit) {
    return discover(user, query, limit, false);
  }

  @Transactional
  public DiscoveryResponse recommendations(long user, String prompt, int limit) {
    return discover(user, prompt, limit, true);
  }

  private DiscoveryResponse discover(long user, String prompt, int limit, boolean personalized) {
    long start = System.nanoTime();
    var catalogue =
        movies.findAll(PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "id"))).getContent();
    var documents =
        catalogue.stream()
            .map(m -> new ContentRecommendationEngine.Document(m.id(), text(m)))
            .toList();
    Set<Long> excluded = new HashSet<>();
    String query = prompt == null ? "" : prompt;
    if (personalized) {
      excluded.addAll(
          sql.list(
              "SELECT i.movie_id FROM collection_item i JOIN user_collection c ON"
                  + " c.id=i.collection_id WHERE c.user_id=?",
              (r, n) -> r.getLong(1),
              user));
      var rated =
          sql.list(
              "SELECT movie_id,score FROM personal_rating WHERE user_id=?",
              (r, n) -> Map.entry(r.getLong(1), r.getInt(2)),
              user);
      var disliked = new HashSet<Long>();
      for (var rating : rated) if (rating.getValue() < 4) disliked.add(rating.getKey());
      var preferred = new HashSet<>(excluded);
      preferred.removeAll(disliked);
      for (var rating : rated) {
        excluded.add(rating.getKey());
        if (rating.getValue() >= 4) preferred.add(rating.getKey());
      }
      var profile = new StringBuilder(query);
      for (var document : documents)
        if (preferred.contains(document.id())) profile.append(" ").append(document.text());
      query = profile.toString();
    }
    var matches = engine.rank(query, documents, excluded, limit);
    boolean fallback = matches.isEmpty();
    List<RankedMovie> results;
    if (fallback)
      results =
          catalogue.stream()
              .filter(m -> !excluded.contains(m.id()))
              .limit(limit)
              .map(
                  m ->
                      new RankedMovie(
                          MovieResponse.from(m),
                          0,
                          "Recent catalogue entry; no confident content match."))
              .toList();
    else {
      var index = new HashMap<Long, Movie>();
      for (var m : catalogue) index.put(m.id(), m);
      results =
          matches.stream()
              .map(
                  match ->
                      new RankedMovie(
                          MovieResponse.from(index.get(match.id())),
                          match.score(),
                          personalized
                              ? "Content similarity to your preferences and saved or highly rated"
                                    + " movies."
                              : "Content similarity to your search terms."))
              .toList();
    }
    String type = personalized ? "RECOMMENDATION" : "NATURAL_LANGUAGE_SEARCH";
    audit.record(user, type, "DISCOVERY", null, "SUCCESS");
    analytics.record(user, personalized ? "recommendation_completed" : "search_completed", null);
    sql.update(
        "INSERT INTO"
            + " ai_request(user_id,request_type,engine,fallback_used,result_count,latency_ms,correlation_id)"
            + " VALUES(?,?,?,?,?,?,?)",
        user,
        type,
        "local-tfidf-v1",
        fallback,
        results.size(),
        (System.nanoTime() - start) / 1_000_000,
        AuditService.correlationId());
    return new DiscoveryResponse(
        "local-tfidf-v1",
        fallback,
        fallback
            ? "No confident match or insufficient history; returning recent unseen catalogue"
                  + " entries."
            : "Local content-based ranking; relevance is a similarity score, not a probability.",
        results);
  }

  private String text(Movie movie) {
    return movie.title()
        + " "
        + Objects.toString(movie.description(), "")
        + " "
        + movie.director()
        + " "
        + movie.genre()
        + " "
        + movie.releaseDate().getYear();
  }
}
