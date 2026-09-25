package com.sms.movies.service;

import com.sms.movies.dto.AccountDtos.*;
import com.sms.movies.repository.*;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
  private final UserRepository users;
  private final SqlRepository sql;
  private final PasswordEncoder passwords;
  private final ActivityReporter activity;

  public AccountService(
      UserRepository users, SqlRepository sql, PasswordEncoder passwords, ActivityReporter activity) {
    this.users = users;
    this.sql = sql;
    this.passwords = passwords;
    this.activity = activity;
  }

  public static String normalizeEmail(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }

  @Transactional
  public UserResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    UUID id =
        users.create(email, passwords.encode(request.password()), request.displayName().strip());
    UUID collection = UUID.randomUUID();
    sql.update(
        "INSERT INTO user_collection(id,user_id,name) VALUES(?,?,?)", collection, id, "My movies");
    activity.audit(id, "ACCOUNT_REGISTERED", "USER", id, "SUCCESS");
    activity.audit(id, "COLLECTION_CREATED", "COLLECTION", collection, "SUCCESS");
    return users.find(email).user();
  }
}
