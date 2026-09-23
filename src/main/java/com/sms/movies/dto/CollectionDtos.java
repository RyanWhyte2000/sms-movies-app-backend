package com.sms.movies.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class CollectionDtos {
  private CollectionDtos() {}

  public record NameRequest(@NotBlank @Size(max = 120) String name) {}

  public record CollectionResponse(long id, String name, Instant createdAt, Instant updatedAt) {}

  public record CollectionDetails(
      CollectionResponse collection, PageResponse<MovieResponse> movies) {}

  public record ShareRequest(@Future Instant expiresAt) {}

  public record ShareResponse(long id, Instant createdAt, Instant expiresAt, Instant revokedAt) {}

  public record CreatedShare(long id, String token, String path, Instant expiresAt) {}

  public record SharedCollection(String name, PageResponse<MovieResponse> movies) {}
}
