package com.sms.movies.controller;

import com.sms.movies.dto.DiscoveryDtos.*;
import com.sms.movies.service.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Genre")
@RestController
@RequestMapping("/api/genres")
public class GenreController {
  private final CatalogService catalog;

  public GenreController(CatalogService catalog) {
    this.catalog = catalog;
  }

  @GetMapping
  public java.util.List<NamedItem> list() {
    return catalog.genres();
  }

  public record NameRequest(@NotBlank @Size(max = 100) String name) {}

  @PostMapping
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public NamedItem create(@Valid @RequestBody NameRequest request) {
    return catalog.createGenre(request.name());
  }
}
