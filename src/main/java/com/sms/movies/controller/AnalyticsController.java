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
    name = "Analytics", description = "Record consented product analytics events.")
@RestController
@RequestMapping("/api/analytics/events")
public class AnalyticsController {
  private final AnalyticsService service;
  private final CurrentUser current;

  public AnalyticsController(AnalyticsService service, CurrentUser current) {
    this.service = service;
    this.current = current;
  }

  @PostMapping
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Record an analytics event",
      description = "Validates and records an analytics event for the authenticated user.")
  public ResponseEntity<Void> accept(
      @Valid @RequestBody com.sms.movies.dto.AnalyticsDtos.EventRequest body) {
    service.accept(current.id(), body);
    return ResponseEntity.accepted().build();
  }
}
