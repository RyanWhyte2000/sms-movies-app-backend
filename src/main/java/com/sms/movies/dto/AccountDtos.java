package com.sms.movies.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class AccountDtos {
  private AccountDtos() {}

  public record RegisterRequest(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 12, max = 128) String password,
      @NotBlank @Size(max = 100) String displayName) {}

  public record LoginRequest(
      @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 128) String password) {}

  public record UserResponse(long id, String email, String displayName, Instant createdAt) {}
}
