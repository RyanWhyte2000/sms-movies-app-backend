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

@io.swagger.v3.oas.annotations.tags.Tag(name = "Movie")
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
  public PageResponse<MovieResponse> list(
      @RequestParam(required = false) @jakarta.validation.constraints.Size(max = 200) String q,
      @RequestParam(required = false) @Positive Long castId,
      @RequestParam(required = false) @Positive Long directorId,
      @RequestParam(required = false) @Positive Long genreId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return catalog.search(current.id(), q, castId, directorId, genreId, page, size);
  }

  @GetMapping("/{id}")
  public com.sms.movies.dto.DiscoveryDtos.MovieDetails get(@PathVariable @Positive long id) {
    return catalog.details(id, current.id());
  }

  @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
  @PostMapping
  public ResponseEntity<MovieResponse> create(@Valid @RequestBody MovieRequest request) {
    MovieResponse movie = service.create(request);
    return ResponseEntity.created(URI.create("/api/movies/" + movie.id())).body(movie);
  }

  @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{id}")
  public MovieResponse update(
      @PathVariable @Positive long id, @Valid @RequestBody MovieRequest request) {
    return service.update(id, request);
  }

  @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable @Positive long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
