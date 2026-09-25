package com.sms.movies.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Ranks catalogue documents without coupling discovery workflows to one algorithm. */
public interface RecommendationEngine {
  record Document(UUID id, String text) {}

  record Match(UUID id, double score) {}

  List<Match> rank(String query, List<Document> documents, Set<UUID> excluded, int limit);

  String name();
}
