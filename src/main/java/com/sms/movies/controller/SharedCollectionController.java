package com.sms.movies.controller;

import com.sms.movies.dto.*;
import com.sms.movies.dto.CollectionDtos.*;
import com.sms.movies.dto.RatingDtos.*;
import com.sms.movies.service.*;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(name = "SharedCollection")
@RestController
@RequestMapping("/api/shared/collections")
public class SharedCollectionController {
  private final ShareService service;

  public SharedCollectionController(ShareService service) {
    this.service = service;
  }

  @GetMapping("/{token}")
  public ResponseEntity<SharedCollection> read(
      @PathVariable @Size(max = 100) String token,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return ResponseEntity.ok()
        .cacheControl(org.springframework.http.CacheControl.noStore())
        .body(service.read(token, page, size));
  }
}
