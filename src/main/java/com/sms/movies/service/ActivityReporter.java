package com.sms.movies.service;

import java.util.UUID;

/** Port used by business workflows to report audit and product activity. */
public interface ActivityReporter {
  void audit(UUID user, String action, String resourceType, Object resource, String outcome);

  void analytics(UUID user, String event, UUID movie);

  String correlationId();
}
