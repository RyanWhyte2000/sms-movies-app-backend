package com.sms.movies.controller;

import com.sms.movies.dto.DiscoveryDtos.*;
import com.sms.movies.security.CurrentUser;
import com.sms.movies.service.DiscoveryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Natural-language Search",
    description = "Discover movies using a natural-language query.")
@RestController
@RequestMapping("/api/search/natural-language")
public class SearchController {
  private final DiscoveryService service;
  private final CurrentUser current;

  public SearchController(DiscoveryService service, CurrentUser current) {
    this.service = service;
    this.current = current;
  }

  @PostMapping
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Search with natural language",
      description = "Interprets a free-text request and returns ranked matching movies.")
  public DiscoveryResponse discover(@Valid @RequestBody SearchRequest body) {
    return service.search(current.id(), body.query(), body.resultLimit());
  }
}
