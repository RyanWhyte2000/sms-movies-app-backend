package com.sms.movies.service;

import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Local TF-IDF/cosine baseline. Text is data; it is never executed or sent to a provider. */
@Component
public class ContentRecommendationEngine {
  public record Document(long id, String text) {}

  public record Match(long id, double score) {}

  private static final Set<String> STOP =
      Set.of(
          "a",
          "an",
          "the",
          "and",
          "or",
          "of",
          "to",
          "in",
          "on",
          "for",
          "with",
          "by",
          "from",
          "me",
          "my",
          "i",
          "want",
          "show",
          "find",
          "movie",
          "movies",
          "film",
          "films",
          "about",
          "recommend",
          "please",
          "some",
          "like",
          "is",
          "it");
  private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");

  public List<Match> rank(String query, List<Document> documents, Set<Long> excluded, int limit) {
    if (documents.isEmpty()) return List.of();
    var tokens = new ArrayList<Map<String, Double>>();
    var frequencies = new HashMap<String, Integer>();
    for (var document : documents) {
      var terms = terms(document.text());
      tokens.add(terms);
      for (String term : terms.keySet()) frequencies.merge(term, 1, Integer::sum);
    }
    var queryVector = weight(terms(query), frequencies, documents.size());
    double queryNorm = norm(queryVector);
    if (queryNorm == 0) return List.of();
    var results = new ArrayList<Match>();
    for (int i = 0; i < documents.size(); i++) {
      if (excluded.contains(documents.get(i).id())) continue;
      var vector = weight(tokens.get(i), frequencies, documents.size());
      double denominator = norm(vector) * queryNorm;
      if (denominator == 0) continue;
      double dot = 0;
      for (var term : queryVector.entrySet())
        dot += term.getValue() * vector.getOrDefault(term.getKey(), 0.0);
      double score = Math.min(1, dot / denominator);
      if (score >= 0.05) results.add(new Match(documents.get(i).id(), score));
    }
    return results.stream()
        .sorted(Comparator.comparingDouble(Match::score).reversed().thenComparingLong(Match::id))
        .limit(limit)
        .toList();
  }

  private Map<String, Double> terms(String text) {
    var result = new HashMap<String, Double>();
    var matcher = WORD.matcher(text == null ? "" : text.toLowerCase(Locale.ROOT));
    while (matcher.find()) {
      String word = matcher.group();
      word =
          switch (word) {
            case "funny", "comedies" -> "comedy";
            case "scary" -> "horror";
            case "romantic" -> "romance";
            default -> word;
          };
      if (!STOP.contains(word)) result.merge(word, 1.0, Double::sum);
    }
    return result;
  }

  private Map<String, Double> weight(
      Map<String, Double> terms, Map<String, Integer> frequencies, int count) {
    var result = new HashMap<String, Double>();
    terms.forEach(
        (term, frequency) -> {
          if (frequencies.containsKey(term))
            result.put(
                term,
                (1 + Math.log(frequency))
                    * (1 + Math.log((1.0 + count) / (1 + frequencies.get(term)))));
        });
    return result;
  }

  private double norm(Map<String, Double> vector) {
    return Math.sqrt(vector.values().stream().mapToDouble(v -> v * v).sum());
  }
}
