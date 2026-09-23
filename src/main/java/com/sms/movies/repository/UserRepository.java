package com.sms.movies.repository;

import com.sms.movies.dto.AccountDtos.UserResponse;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
  public record Account(UserResponse user, String passwordHash, String role) {}

  private final SqlRepository sql;

  public UserRepository(SqlRepository sql) {
    this.sql = sql;
  }

  public Account find(String email) {
    return sql.one(
        "SELECT * FROM app_user WHERE email=?",
        (r, n) ->
            new Account(
                new UserResponse(
                    r.getLong("id"),
                    r.getString("email"),
                    r.getString("display_name"),
                    r.getTimestamp("created_at").toInstant()),
                r.getString("password_hash"),
                r.getString("role")),
        email);
  }

  public long create(String email, String hash, String name) {
    return sql.insert(
        "INSERT INTO app_user(email,password_hash,display_name) VALUES(?,?,?)", email, hash, name);
  }

  public void lock(long id) {
    sql.one("SELECT id FROM app_user WHERE id=? FOR UPDATE", (r, n) -> r.getLong(1), id);
  }
}
