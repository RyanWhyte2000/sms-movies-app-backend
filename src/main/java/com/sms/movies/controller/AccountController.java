package com.sms.movies.controller;

import com.sms.movies.dto.AccountDtos.UserResponse;
import com.sms.movies.security.CurrentUser;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Account", description = "Read information about the authenticated user account.")
@RestController
@RequestMapping("/api/me")
public class AccountController {
  private final CurrentUser current;

  public AccountController(CurrentUser current) {
    this.current = current;
  }

  @GetMapping
  @io.swagger.v3.oas.annotations.Operation(
      summary = "Get current user",
      description = "Returns the public account details for the authenticated user.")
  public UserResponse me() {
    return current.get();
  }
}
