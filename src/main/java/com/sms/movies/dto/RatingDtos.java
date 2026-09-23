package com.sms.movies.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class RatingDtos {
  private RatingDtos() {}

  public record RatingRequest(@NotNull @Min(1) @Max(5) Integer score) {}

  public record RatingResponse(long movieId, int score, Instant createdAt, Instant updatedAt) {}
}
