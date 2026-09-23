package com.sms.movies.security;

import com.sms.movies.dto.AccountDtos.UserResponse;
import com.sms.movies.exception.ApiException;
import com.sms.movies.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
  private final UserRepository users;

  public CurrentUser(UserRepository users) {
    this.users = users;
  }

  public UserResponse get() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var account = authentication == null ? null : users.find(authentication.getName());
    if (account == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
    return account.user();
  }

  public long id() {
    return get().id();
  }
}
