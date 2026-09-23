package com.sms.movies.controller;

import com.sms.movies.dto.*;
import com.sms.movies.dto.CollectionDtos.*;
import com.sms.movies.dto.RatingDtos.*;
import com.sms.movies.security.CurrentUser;
import com.sms.movies.service.*;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Audit")
@RestController
@RequestMapping("/api/me/audit-events")
public class AuditController {
  private final AuditService service;
  private final CurrentUser current;

  public AuditController(AuditService service, CurrentUser current) {
    this.service = service;
    this.current = current;
  }

  @GetMapping
  public PageResponse<AuditService.AuditEvent> list(
      @RequestParam(required = false) @Size(max = 80) String action,
      @RequestParam(required = false) java.time.Instant from,
      @RequestParam(required = false) java.time.Instant to,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    if (from != null && to != null && from.isAfter(to))
      throw new com.sms.movies.exception.ApiException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "from must not be after to.");
    return service.list(current.id(), action, from, to, page, size);
  }
}
