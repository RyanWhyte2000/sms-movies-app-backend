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

@io.swagger.v3.oas.annotations.tags.Tag(name = "Share")
@RestController
@RequestMapping("/api/me/collections/{collectionId}/shares")
public class ShareController {
  private final ShareService service;
  private final CurrentUser current;

  public ShareController(ShareService service, CurrentUser current) {
    this.service = service;
    this.current = current;
  }

  @PostMapping
  public ResponseEntity<CreatedShare> create(
      @PathVariable @Positive long collectionId,
      @Valid @RequestBody(required = false) ShareRequest body) {
    var result = service.create(current.id(), collectionId, body == null ? null : body.expiresAt());
    return ResponseEntity.created(java.net.URI.create(result.path())).body(result);
  }

  @GetMapping
  public java.util.List<ShareResponse> list(@PathVariable @Positive long collectionId) {
    return service.list(current.id(), collectionId);
  }

  @DeleteMapping("/{shareId}")
  public ResponseEntity<Void> revoke(
      @PathVariable @Positive long collectionId, @PathVariable @Positive long shareId) {
    service.revoke(current.id(), collectionId, shareId);
    return ResponseEntity.noContent().build();
  }
}
