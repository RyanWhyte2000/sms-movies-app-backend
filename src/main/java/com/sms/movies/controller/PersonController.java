package com.sms.movies.controller;

import com.sms.movies.dto.DiscoveryDtos.*;
import com.sms.movies.dto.PageResponse;
import com.sms.movies.service.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Person")
@RestController
@RequestMapping("/api/people")
public class PersonController {
  private final CatalogService catalog;

  public PersonController(CatalogService catalog) {
    this.catalog = catalog;
  }

  @GetMapping
  public PageResponse<NamedItem> list(
      @RequestParam(defaultValue = "") @Size(max = 200) String q,
      @RequestParam(required = false) String role,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return catalog.people(q, role, page, size);
  }

  public record NameRequest(@NotBlank @Size(max = 200) String name) {}

  @PostMapping
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public NamedItem create(@Valid @RequestBody NameRequest request) {
    return catalog.createPerson(request.name());
  }
}
