package com.sms.movies.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public final class AnalyticsDtos {
  private AnalyticsDtos() {}

  public enum ClientEvent {
    SEARCH_RESULT_SELECTED,
    RECOMMENDATION_VIEWED,
    RECOMMENDATION_SELECTED
  }

  public record EventRequest(
      @NotNull UUID eventId,
      @NotNull ClientEvent eventName,
      @NotNull java.util.UUID movieId,
      UUID journeyId,
      @NotNull @AssertTrue Boolean consentGranted) {}
}
