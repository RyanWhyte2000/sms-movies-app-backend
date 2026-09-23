package com.sms.movies.service;

import com.sms.movies.dto.AccountDtos.*;
import com.sms.movies.repository.*;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
  private final UserRepository users;
  private final SqlRepository sql;
  private final PasswordEncoder passwords;
  private final AuditService audit;

  public AccountService(
      UserRepository users, SqlRepository sql, PasswordEncoder passwords, AuditService audit) {
    this.users = users;
    this.sql = sql;
    this.passwords = passwords;
    this.audit = audit;
  }

  public static String normalizeEmail(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }

  @Transactional
  public UserResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    long id =
        users.create(email, passwords.encode(request.password()), request.displayName().strip());
    long collection =
        sql.insert("INSERT INTO user_collection(user_id,name) VALUES(?,?)", id, "My movies");
    audit.record(id, "ACCOUNT_REGISTERED", "USER", id, "SUCCESS");
    audit.record(id, "COLLECTION_CREATED", "COLLECTION", collection, "SUCCESS");
    return users.find(email).user();
  }
}
