package com.sms.movies.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContentRecommendationEngineTest {
  private final RecommendationEngine engine = new ContentRecommendationEngine();

  @Test
  void ranksRelevantDocumentsAndHonorsExclusions() {
    UUID comedy = UUID.randomUUID();
    UUID horror = UUID.randomUUID();
    var documents =
        List.of(
            new RecommendationEngine.Document(comedy, "funny family comedy"),
            new RecommendationEngine.Document(horror, "scary haunted horror"));

    var matches = engine.rank("a funny movie", documents, Set.of(horror), 10);

    assertEquals(1, matches.size());
    assertEquals(comedy, matches.get(0).id());
    assertTrue(matches.get(0).score() > 0);
  }

  @Test
  void returnsNoMatchesForStopWordsOnly() {
    var documents =
        List.of(new RecommendationEngine.Document(UUID.randomUUID(), "science fiction"));

    assertTrue(engine.rank("please show me a movie", documents, Set.of(), 10).isEmpty());
  }
}
