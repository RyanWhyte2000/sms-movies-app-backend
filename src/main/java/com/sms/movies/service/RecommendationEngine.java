package com.sms.movies.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Ranks catalogue documents without coupling discovery workflows to one algorithm. */
public interface RecommendationEngine {
  record Document(UUID id, String text) {}

  record Match(UUID id, double score, String explanation) {
    public Match(UUID id, double score) {
      this(id, score, null);
    }
  }

  record Outcome(String engine, boolean fallbackUsed, String message, List<Match> matches) {}

  List<Match> rank(String query, List<Document> documents, Set<UUID> excluded, int limit);

  default Outcome rankWithStatus(
      String query, List<Document> documents, Set<UUID> excluded, int limit) {
    return new Outcome(name(), false, "Ranked results.", rank(query, documents, excluded, limit));
  }

  String name();
}
