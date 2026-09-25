package com.sms.movies.controller;

import com.sms.movies.dto.MovieRequest;
import com.sms.movies.dto.MovieResponse;
import com.sms.movies.dto.PageResponse;
import com.sms.movies.service.MovieService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Movies", description = "Search, inspect and administer the movie catalogue.")
@RestController
@RequestMapping("/api/movies")
public class MovieController {
  private final MovieService service;
  private final com.sms.movies.service.CatalogService catalog;
  private final com.sms.movies.security.CurrentUser current;

  public MovieController(
      MovieService service,
      com.sms.movies.service.CatalogService catalog,
      com.sms.movies.security.CurrentUser current) {
    this.service = service;
    this.catalog = catalog;
    this.current = current;
  }

  @GetMapping
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Search movies",
      description = "Returns a paginated movie catalogue filtered by text, cast, director or genre.")
  public PageResponse<MovieResponse> list(
      @RequestParam(required = false) @jakarta.validation.constraints.Size(max = 200) String q,
      @RequestParam(required = false) java.util.UUID castId,
      @RequestParam(required = false) java.util.UUID directorId,
      @RequestParam(required = false) java.util.UUID genreId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return catalog.search(current.id(), q, castId, directorId, genreId, page, size);
  }

  @GetMapping("/{id}")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Get movie details",
      description = "Returns catalogue details, credits, genres, external ratings and personal rating.")
  public com.sms.movies.dto.DiscoveryDtos.MovieDetails get(@PathVariable java.util.UUID id) {
    return catalog.details(id, current.id());
  }

  @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Create a movie",
      description = "Adds a movie to the catalogue. Requires administrator access.")
  public ResponseEntity<MovieResponse> create(@Valid @RequestBody MovieRequest request) {
    MovieResponse movie = service.create(request);
    return ResponseEntity.created(URI.create("/api/movies/" + movie.id())).body(movie);
  }

  @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Update a movie",
      description = "Replaces a movie's catalogue details. Requires administrator access.")
  public MovieResponse update(
      @PathVariable java.util.UUID id, @Valid @RequestBody MovieRequest request) {
    return service.update(id, request);
  }

  @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Delete a movie",
      description = "Removes a movie from the catalogue. Requires administrator access.")
  public ResponseEntity<Void> delete(@PathVariable java.util.UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
