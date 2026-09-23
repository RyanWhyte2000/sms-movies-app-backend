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

@io.swagger.v3.oas.annotations.tags.Tag(name = "Rating")
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
  public PageResponse<RatingResponse> list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.list(current.id(), page, size);
  }

  @PutMapping("/{movieId}")
  public RatingResponse put(
      @PathVariable @Positive long movieId, @Valid @RequestBody RatingRequest body) {
    return service.put(current.id(), movieId, body.score());
  }

  @DeleteMapping("/{movieId}")
  public ResponseEntity<Void> delete(@PathVariable @Positive long movieId) {
    service.delete(current.id(), movieId);
    return ResponseEntity.noContent().build();
  }
}
