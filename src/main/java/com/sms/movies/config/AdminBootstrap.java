package com.sms.movies.config;

import com.sms.movies.dto.AccountDtos.RegisterRequest;
import com.sms.movies.repository.*;
import com.sms.movies.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {
  private final String email, password;
  private final AccountService accounts;
  private final UserRepository users;
  private final SqlRepository sql;

  public AdminBootstrap(
      @Value("${app.admin.email:}") String email,
      @Value("${app.admin.password:}") String password,
      AccountService accounts,
      UserRepository users,
      SqlRepository sql) {
    this.email = email;
    this.password = password;
    this.accounts = accounts;
    this.users = users;
    this.sql = sql;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments arguments) {
    if (email.isBlank() && password.isBlank()) return;
    if (email.isBlank() || password.length() < 12 || password.length() > 128)
      throw new IllegalStateException(
          "Bootstrap admin requires an email and a password of 12 to 128 characters.");
    var account = users.find(AccountService.normalizeEmail(email));
    if (account != null) {
      if (!account.role().equals("ADMIN"))
        throw new IllegalStateException(
            "Bootstrap email belongs to a non-admin account; refusing automatic promotion.");
      return;
    }
    var created = accounts.register(new RegisterRequest(email, password, "Administrator"));
    sql.update("UPDATE app_user SET role='ADMIN' WHERE id=?", created.id());
  }
}
