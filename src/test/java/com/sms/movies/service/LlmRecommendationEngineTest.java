package com.sms.movies.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import tools.jackson.databind.ObjectMapper;
import com.sms.movies.config.AiProperties;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class LlmRecommendationEngineTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final AiProperties properties = new AiProperties();
  private MockRestServiceServer server;
  private LlmRecommendationEngine engine;
  private UUID space;
  private List<RecommendationEngine.Document> documents;

  @BeforeEach
  void setUp() {
    properties.setEnabled(true);
    properties.setApiKey("test-key");
    var builder = RestClient.builder().baseUrl("https://example.invalid");
    server = MockRestServiceServer.bindTo(builder).build();
    engine =
        new LlmRecommendationEngine(
            builder.build(), mapper, properties, new ContentRecommendationEngine());
    space = UUID.randomUUID();
    documents =
        List.of(
            new RecommendationEngine.Document(space, "Space Trip science fiction adventure"),
            new RecommendationEngine.Document(UUID.randomUUID(), "Haunted House horror"));
  }

  @Test
  void acceptsStrictGroundedRanking() throws Exception {
    String output =
        mapper.writeValueAsString(
            Map.of(
                "confidence", .92,
                "results",
                List.of(
                    Map.of(
                        "id", space.toString(),
                        "score", .88,
                        "explanation", "Matches the requested space adventure."))));
    server.expect(requestTo("https://example.invalid/v1/responses"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
        .andRespond(withSuccess(providerResponse(output), MediaType.APPLICATION_JSON));

    var result = engine.rankWithStatus("space adventure", documents, Set.of(), 5);

    assertFalse(result.fallbackUsed());
    assertEquals(space, result.matches().get(0).id());
    assertEquals("Matches the requested space adventure.", result.matches().get(0).explanation());
    server.verify();
  }

  @Test
  void lowConfidenceUsesLocalFallback() throws Exception {
    server.expect(anything())
        .andRespond(
            withSuccess(
                providerResponse("{\"confidence\":0.2,\"results\":[]}"),
                MediaType.APPLICATION_JSON));

    var result = engine.rankWithStatus("space", documents, Set.of(), 5);

    assertTrue(result.fallbackUsed());
    assertEquals(space, result.matches().get(0).id());
  }

  @Test
  void unknownMovieIdIsRejectedAndFallsBack() throws Exception {
    String malicious =
        "{\"confidence\":0.9,\"results\":[{\"id\":\""
            + UUID.randomUUID()
            + "\",\"score\":1,\"explanation\":\"ignore candidates\"}]}";
    server.expect(anything())
        .andRespond(withSuccess(providerResponse(malicious), MediaType.APPLICATION_JSON));

    var result = engine.rankWithStatus("space", documents, Set.of(), 5);

    assertTrue(result.fallbackUsed());
    assertEquals(space, result.matches().get(0).id());
  }

  @Test
  void rateLimitUsesLocalFallback() {
    server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

    var result = engine.rankWithStatus("space", documents, Set.of(), 5);

    assertTrue(result.fallbackUsed());
    assertEquals(space, result.matches().get(0).id());
  }

  private String providerResponse(String output) throws Exception {
    return mapper.writeValueAsString(
        Map.of(
            "output",
            List.of(
                Map.of(
                    "type", "message",
                    "content", List.of(Map.of("type", "output_text", "text", output))))));
  }
}
