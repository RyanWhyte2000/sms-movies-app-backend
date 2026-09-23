package com.sms.movies.controller;

import com.sms.movies.dto.DiscoveryDtos.*;
import com.sms.movies.security.CurrentUser;
import com.sms.movies.service.DiscoveryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Recommendation")
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
  private final DiscoveryService service;
  private final CurrentUser current;

  public RecommendationController(DiscoveryService service, CurrentUser current) {
    this.service = service;
    this.current = current;
  }

  @PostMapping
  public DiscoveryResponse discover(@Valid @RequestBody RecommendationRequest body) {
    return service.recommendations(current.id(), body.prompt(), body.resultLimit());
  }
}
