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

@io.swagger.v3.oas.annotations.tags.Tag(name = "Collection")
@RestController
@RequestMapping("/api/me/collections")
public class CollectionController {
  private final CollectionService service;
  private final CurrentUser current;

  public CollectionController(CollectionService service, CurrentUser current) {
    this.service = service;
    this.current = current;
  }

  @GetMapping
  public PageResponse<CollectionResponse> list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.list(current.id(), page, size);
  }

  @PostMapping
  public ResponseEntity<CollectionResponse> create(@Valid @RequestBody NameRequest body) {
    var result = service.create(current.id(), body.name());
    return ResponseEntity.created(java.net.URI.create("/api/me/collections/" + result.id()))
        .body(result);
  }

  @GetMapping("/{id}")
  public CollectionDetails get(
      @PathVariable @Positive long id,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.get(current.id(), id, page, size);
  }

  @PatchMapping("/{id}")
  public CollectionResponse rename(
      @PathVariable @Positive long id, @Valid @RequestBody NameRequest body) {
    return service.rename(current.id(), id, body.name());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable @Positive long id) {
    service.delete(current.id(), id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{id}/movies/{movieId}")
  public ResponseEntity<Void> add(
      @PathVariable @Positive long id, @PathVariable @Positive long movieId) {
    service.add(current.id(), id, movieId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}/movies/{movieId}")
  public ResponseEntity<Void> remove(
      @PathVariable @Positive long id, @PathVariable @Positive long movieId) {
    service.remove(current.id(), id, movieId);
    return ResponseEntity.noContent().build();
  }
}
