package com.sms.movies.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class DiscoveryDtos {
  private DiscoveryDtos() {}

  public record NamedItem(long id, String name) {}

  public record Credit(long personId, String name, String role) {}

  public record ExternalRating(
      String source, double score, double scaleMax, Long voteCount, Instant fetchedAt) {}

  public record ExternalRatingRequest(
      @NotNull @DecimalMin("0.0") Double score,
      @NotNull @DecimalMin(value = "0.0", inclusive = false) Double scaleMax,
      @PositiveOrZero Long voteCount) {}

  public record MovieDetails(
      long id,
      String title,
      String description,
      String director,
      String genre,
      java.time.LocalDate releaseDate,
      Integer durationMinutes,
      List<Credit> credits,
      List<NamedItem> genres,
      List<ExternalRating> externalRatings,
      Integer personalRating) {}

  public record SearchRequest(
      @NotBlank @Size(max = 1000) String query, @Min(1) @Max(20) Integer limit) {
    public int resultLimit() {
      return limit == null ? 10 : limit;
    }
  }

  public record RecommendationRequest(
      @Size(max = 1000) String prompt, @Min(1) @Max(20) Integer limit) {
    public int resultLimit() {
      return limit == null ? 10 : limit;
    }
  }

  public record RankedMovie(MovieResponse movie, double relevance, String explanation) {}

  public record DiscoveryResponse(
      String engine, boolean fallbackUsed, String message, List<RankedMovie> results) {}
}
