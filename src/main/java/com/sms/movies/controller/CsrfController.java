package com.sms.movies.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Csrf")
@RestController
public class CsrfController {
  @GetMapping("/api/csrf")
  public TokenResponse token(CsrfToken token) {
    return new TokenResponse(token.getHeaderName(), token.getToken());
  }

  public record TokenResponse(String headerName, String token) {}
}
