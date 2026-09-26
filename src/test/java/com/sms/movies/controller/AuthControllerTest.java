package com.sms.movies.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.movies.dto.AccountDtos.RegisterRequest;
import com.sms.movies.dto.AccountDtos.UserResponse;
import com.sms.movies.repository.UserRepository;
import com.sms.movies.security.CurrentUser;
import com.sms.movies.service.AccountService;
import com.sms.movies.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;

class AuthControllerTest {
  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void registrationCreatesAuthenticatedSession() {
    AccountService accounts = mock(AccountService.class);
    AuthenticationManager authentication = mock(AuthenticationManager.class);
    SecurityContextRepository contexts = mock(SecurityContextRepository.class);
    SessionAuthenticationStrategy sessions = mock(SessionAuthenticationStrategy.class);
    CurrentUser current = mock(CurrentUser.class);
    AuditService audit = mock(AuditService.class);
    UserRepository users = mock(UserRepository.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    Authentication authenticated = mock(Authentication.class);
    var body = new RegisterRequest(" New@Example.com ", "long-password", "New User");
    var user = new UserResponse(UUID.randomUUID(), "new@example.com", "New User", Instant.now());
    var account = new UserRepository.Account(user, "hash", "USER");

    when(accounts.register(body)).thenReturn(user);
    when(authentication.authenticate(any())).thenReturn(authenticated);
    when(users.find("new@example.com")).thenReturn(account);

    var controller =
        new AuthController(accounts, authentication, contexts, sessions, current, audit, users);
    var result = controller.register(body, request, response);

    assertEquals(201, result.getStatusCode().value());
    assertSame(user, result.getBody());
    verify(sessions).onAuthentication(authenticated, request, response);
    verify(contexts).saveContext(any(), any(), any());
    assertSame(authenticated, SecurityContextHolder.getContext().getAuthentication());
  }
}
