package com.sms.movies.controller;

import com.sms.movies.dto.DiscoveryDtos.*;
import com.sms.movies.service.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(
    name = "External Ratings",
    description = "Maintain ratings imported from external providers.")
@RestController
@RequestMapping("/api/movies/{movieId}/external-ratings")
public class ExternalRatingController {
  private final CatalogService catalog;
  private final com.sms.movies.security.CurrentUser current;

  public ExternalRatingController(
      CatalogService catalog, com.sms.movies.security.CurrentUser current) {
    this.catalog = catalog;
    this.current = current;
  }

  @PutMapping("/{source}")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Save an external rating",
      description = "Creates or updates a provider rating for a movie. Requires administrator access.")
  @PreAuthorize("hasRole('ADMIN')")
  public ExternalRating put(
      @PathVariable java.util.UUID movieId,
      @PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,80}") String source,
      @Valid @RequestBody ExternalRatingRequest request) {
    return catalog.putExternal(current.id(), movieId, source, request);
  }
}
