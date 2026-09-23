package com.sms.movies.controller;

import com.sms.movies.dto.DiscoveryDtos.*;
import com.sms.movies.security.CurrentUser;
import com.sms.movies.service.DiscoveryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Search")
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
  public DiscoveryResponse discover(@Valid @RequestBody SearchRequest body) {
    return service.search(current.id(), body.query(), body.resultLimit());
  }
}
