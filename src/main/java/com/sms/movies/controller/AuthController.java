package com.sms.movies.controller;

import com.sms.movies.dto.AccountDtos.*;
import com.sms.movies.repository.UserRepository;
import com.sms.movies.security.CurrentUser;
import com.sms.movies.service.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AccountService accounts;
  private final AuthenticationManager authentication;
  private final SecurityContextRepository contexts;
  private final SessionAuthenticationStrategy sessions;
  private final CurrentUser current;
  private final AuditService audit;
  private final UserRepository users;

  public AuthController(
      AccountService accounts,
      AuthenticationManager authentication,
      SecurityContextRepository contexts,
      SessionAuthenticationStrategy sessions,
      CurrentUser current,
      AuditService audit,
      UserRepository users) {
    this.accounts = accounts;
    this.authentication = authentication;
    this.contexts = contexts;
    this.sessions = sessions;
    this.current = current;
    this.audit = audit;
    this.users = users;
  }

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest body) {
    return ResponseEntity.status(HttpStatus.CREATED).body(accounts.register(body));
  }

  @PostMapping("/login")
  public UserResponse login(
      @Valid @RequestBody LoginRequest body,
      HttpServletRequest request,
      HttpServletResponse response) {
    String email = AccountService.normalizeEmail(body.email());
    org.springframework.security.core.Authentication result;
    try {
      result =
          authentication.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(email, body.password()));
    } catch (AuthenticationException exception) {
      var user = users.find(email);
      audit.record(user == null ? null : user.user().id(), "LOGIN", "USER", null, "FAILURE");
      throw new com.sms.movies.exception.ApiException(
          HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }
    var account = users.find(email);
    audit.record(account.user().id(), "LOGIN", "USER", account.user().id(), "SUCCESS");
    sessions.onAuthentication(result, request, response);
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(result);
    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, request, response);
    return current.get();
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
    audit.record(current.id(), "LOGOUT", "USER", current.id(), "SUCCESS");
    new SecurityContextLogoutHandler()
        .logout(request, response, SecurityContextHolder.getContext().getAuthentication());
    return ResponseEntity.noContent().build();
  }
}
