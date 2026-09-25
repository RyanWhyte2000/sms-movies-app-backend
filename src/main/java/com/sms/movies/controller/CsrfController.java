package com.sms.movies.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@io.swagger.v3.oas.annotations.tags.Tag(
    name = "CSRF", description = "Obtain CSRF protection details for state-changing requests.")
@RestController
public class CsrfController {
  @GetMapping("/api/csrf")
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Get a CSRF token",
      description = "Returns the header name and token required for protected write requests.")
  public TokenResponse token(CsrfToken token) {
    return new TokenResponse(token.getHeaderName(), token.getToken());
  }

  public record TokenResponse(String headerName, String token) {}
}
