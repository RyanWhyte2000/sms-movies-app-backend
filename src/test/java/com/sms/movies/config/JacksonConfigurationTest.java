package com.sms.movies.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sms.movies.service.LlmRecommendationEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class JacksonConfigurationTest {

  @Autowired private ObjectMapper objectMapper;

  @Autowired private LlmRecommendationEngine recommendationEngine;

  @Test
  void applicationProvidesJacksonMapperToRecommendationEngine() {
    assertNotNull(objectMapper);
    assertNotNull(recommendationEngine);
  }
}
