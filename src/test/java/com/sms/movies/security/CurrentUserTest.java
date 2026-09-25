package com.sms.movies.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sms.movies.dto.AccountDtos.UserResponse;
import com.sms.movies.exception.ApiException;
import com.sms.movies.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserTest {
  @AfterEach void clear() { SecurityContextHolder.clearContext(); }

  @Test
  void resolvesAuthenticatedUserAndAnonymousStates() {
    var users = mock(UserRepository.class);
    var id = UUID.randomUUID();
    var response = new UserResponse(id, "user@example.com", "User", Instant.now());
    when(users.find("user@example.com"))
        .thenReturn(new UserRepository.Account(response, "hash", "USER"));
    var current = new CurrentUser(users);

    SecurityContextHolder.getContext().setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated("user@example.com", "x", List.of()));
    assertEquals(response, current.get());
    assertEquals(id, current.id());
    assertEquals(id, current.idOrNull());

    SecurityContextHolder.clearContext();
    assertNull(current.idOrNull());
    assertThrows(ApiException.class, current::get);

    SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
        "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
    assertNull(current.idOrNull());

    SecurityContextHolder.getContext().setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated("missing@example.com", "x", List.of()));
    assertThrows(ApiException.class, current::idOrNull);
  }
}
