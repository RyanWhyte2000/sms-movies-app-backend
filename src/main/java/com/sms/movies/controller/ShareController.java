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
    name = "Collection Shares",
    description = "Create, inspect and revoke share links for owned collections.")
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
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Create a share link",
      description = "Creates a public share link for an owned collection, optionally with an expiry.")
  public ResponseEntity<CreatedShare> create(
      @PathVariable @Positive long collectionId,
      @Valid @RequestBody(required = false) ShareRequest body) {
    var result = service.create(current.id(), collectionId, body == null ? null : body.expiresAt());
    return ResponseEntity.created(java.net.URI.create(result.path())).body(result);
  }

  @GetMapping
  @io.swagger.v3.oas.annotations.Operation(
      summary = "List collection shares",
      description = "Returns active share-link metadata for an owned collection without tokens.")
  public java.util.List<ShareResponse> list(@PathVariable @Positive long collectionId) {
    return service.list(current.id(), collectionId);
  }

  @DeleteMapping("/{shareId}")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Revoke a share link",
      description = "Revokes an active share link belonging to an owned collection.")
  public ResponseEntity<Void> revoke(
      @PathVariable @Positive long collectionId, @PathVariable @Positive long shareId) {
    service.revoke(current.id(), collectionId, shareId);
    return ResponseEntity.noContent().build();
  }
}
