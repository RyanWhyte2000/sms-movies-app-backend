package com.sms.movies.service;

import static org.junit.jupiter.api.Assertions.*;

import tools.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AiEvaluationSetTest {
  @Test
  void evaluationSetHasCoverageAndStableUniqueIds() throws Exception {
    var stream = getClass().getResourceAsStream("/ai-evaluation-set.json");
    assertNotNull(stream);
    var cases = new ObjectMapper().readTree(stream);
    assertTrue(cases.isArray());
    assertTrue(cases.size() >= 10);
    var ids = new java.util.HashSet<String>();
    var risks = new java.util.HashSet<String>();
    var types = new java.util.HashSet<String>();
    cases.forEach(c -> {
      assertTrue(ids.add(c.path("id").asText()), "duplicate evaluation id");
      assertFalse(c.path("input").asText().isBlank());
      risks.add(c.path("risk").asText());
      types.add(c.path("type").asText());
    });
    assertEquals(Set.of("search", "recommendation"), types);
    assertTrue(risks.containsAll(Set.of("normal", "low-confidence", "prompt-injection", "metadata-injection", "privacy")));
  }
}
