package com.sms.movies.controller;

import com.sms.movies.dto.*;
import com.sms.movies.dto.CollectionDtos.*;
import com.sms.movies.dto.RatingDtos.*;
import com.sms.movies.security.CurrentUser;
import com.sms.movies.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Personal Ratings",
    description = "Manage the authenticated user's one-per-movie personal ratings.")
@RestController
@RequestMapping("/api/me/ratings")
public class RatingController {
  private final RatingService service;
  private final CurrentUser current;

  public RatingController(RatingService service, CurrentUser current) {
    this.service = service;
    this.current = current;
  }

  @GetMapping
  @io.swagger.v3.oas.annotations.Operation(
      summary = "List personal ratings",
      description = "Returns a paginated list of ratings submitted by the authenticated user.")
  public PageResponse<RatingResponse> list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.list(current.id(), page, size);
  }

  @PutMapping("/{movieId}")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Save a personal rating",
      description = "Creates or updates the authenticated user's rating for a movie.")
  public RatingResponse put(
      @PathVariable @Positive long movieId, @Valid @RequestBody RatingRequest body) {
    return service.put(current.id(), movieId, body.score());
  }

  @DeleteMapping("/{movieId}")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Delete a personal rating",
      description = "Removes the authenticated user's rating for a movie.")
  public ResponseEntity<Void> delete(@PathVariable @Positive long movieId) {
    service.delete(current.id(), movieId);
    return ResponseEntity.noContent().build();
  }
}
