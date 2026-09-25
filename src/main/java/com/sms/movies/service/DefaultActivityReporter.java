package com.sms.movies.service;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DefaultActivityReporter implements ActivityReporter {
  private final AuditService auditService;
  private final AnalyticsService analyticsService;

  public DefaultActivityReporter(AuditService auditService, AnalyticsService analyticsService) {
    this.auditService = auditService;
    this.analyticsService = analyticsService;
  }

  @Override
  public void audit(
      UUID user, String action, String resourceType, Object resource, String outcome) {
    auditService.record(user, action, resourceType, resource, outcome);
  }

  @Override
  public void analytics(UUID user, String event, UUID movie) {
    analyticsService.record(user, event, movie);
  }

  @Override
  public String correlationId() {
    return AuditService.correlationId();
  }
}
