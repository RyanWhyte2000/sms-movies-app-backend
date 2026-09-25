package com.sms.movies.controller;

import com.sms.movies.dto.*;
import com.sms.movies.dto.CollectionDtos.*;
import com.sms.movies.dto.RatingDtos.*;
import com.sms.movies.security.CurrentUser;
import com.sms.movies.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Collection",
    description =
        "Manage the authenticated user's movie collections and the movies contained in them.")
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
  @Operation(
      summary = "List collections",
      description = "Returns a paginated list of collections owned by the authenticated user.")
  public PageResponse<CollectionResponse> list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.list(current.id(), page, size);
  }

  @PostMapping
  @Operation(
      summary = "Create a collection",
      description = "Creates a named movie collection for the authenticated user.")
  public ResponseEntity<CollectionResponse> create(@Valid @RequestBody NameRequest body) {
    var result = service.create(current.id(), body.name());
    return ResponseEntity.created(java.net.URI.create("/api/me/collections/" + result.id()))
        .body(result);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Get a collection",
      description = "Returns an owned collection and a paginated list of its movies.")
  public CollectionDetails get(
      @PathVariable java.util.UUID id,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return service.get(current.id(), id, page, size);
  }

  @PatchMapping("/{id}")
  @Operation(
      summary = "Rename a collection",
      description = "Changes the name of a collection owned by the authenticated user.")
  public CollectionResponse rename(
      @PathVariable java.util.UUID id, @Valid @RequestBody NameRequest body) {
    return service.rename(current.id(), id, body.name());
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Delete a collection",
      description = "Deletes an owned collection and its membership entries.")
  public ResponseEntity<Void> delete(@PathVariable java.util.UUID id) {
    service.delete(current.id(), id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{id}/movies/{movieId}")
  @Operation(
      summary = "Add a movie to a collection",
      description = "Adds a movie to an owned collection. Repeating the request is idempotent.")
  public ResponseEntity<Void> add(
      @PathVariable java.util.UUID id, @PathVariable java.util.UUID movieId) {
    service.add(current.id(), id, movieId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}/movies/{movieId}")
  @Operation(
      summary = "Remove a movie from a collection",
      description = "Removes a movie from an owned collection. Repeating the request is idempotent.")
  public ResponseEntity<Void> remove(
      @PathVariable java.util.UUID id, @PathVariable java.util.UUID movieId) {
    service.remove(current.id(), id, movieId);
    return ResponseEntity.noContent().build();
  }
}
