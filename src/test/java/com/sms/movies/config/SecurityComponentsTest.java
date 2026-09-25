package com.sms.movies.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sms.movies.dto.AccountDtos.UserResponse;
import com.sms.movies.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityComponentsTest {
  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void securityFactoriesAuthenticateKnownUsers() {
    var repository = mock(UserRepository.class);
    var id = UUID.randomUUID();
    when(repository.find("user@example.com"))
        .thenReturn(new UserRepository.Account(
            new UserResponse(id, "user@example.com", "User", Instant.now()), "hash", "ADMIN"));
    var config = new SecurityConfig();
    var details = config.userDetailsService(repository).loadUserByUsername("user@example.com");
    assertEquals("user@example.com", details.getUsername());
    assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
        () -> config.userDetailsService(repository).loadUserByUsername("missing@example.com"));

    assertNotNull(config.passwordEncoder());
    assertNotNull(config.authenticationManager(config.userDetailsService(repository), config.passwordEncoder()));
    assertNotNull(config.securityContextRepository());
    assertNotNull(config.csrfTokenRepository());
    assertNotNull(config.sessionAuthenticationStrategy(config.csrfTokenRepository()));
  }

  @Test
  void corsRejectsWildcardAndAcceptsExplicitOrigins() {
    var config = new SecurityConfig();
    assertNotNull(config.corsConfigurationSource("https://example.com, https://app.example.com"));
    assertNotNull(config.corsConfigurationSource(""));
    assertThrows(IllegalArgumentException.class, () -> config.corsConfigurationSource("*"));
  }

  @Test
  void correlationFilterAddsAndCleansMdc() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Correlation-ID", "client-correlation");
    var response = new MockHttpServletResponse();
    var filter = new CorrelationFilter();
    filter.doFilter(request, response, (req, res) -> {
      assertNotNull(org.slf4j.MDC.get("correlationId"));
      assertEquals(response.getHeader("X-Correlation-ID"), org.slf4j.MDC.get("correlationId"));
    });
    assertNotNull(response.getHeader("X-Correlation-ID"));
    assertNull(org.slf4j.MDC.get("correlationId"));
  }
}
