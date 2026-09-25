package com.sms.movies.repository;

import com.sms.movies.dto.AccountDtos.UserResponse;
import java.util.UUID;
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
                    r.getObject("id", UUID.class),
                    r.getString("email"),
                    r.getString("display_name"),
                    r.getTimestamp("created_at").toInstant()),
                r.getString("password_hash"),
                r.getString("role")),
        email);
  }

  public UUID create(String email, String hash, String name) {
    UUID id = UUID.randomUUID();
    sql.update(
        "INSERT INTO app_user(id,email,password_hash,display_name) VALUES(?,?,?,?)",
        id,
        email,
        hash,
        name);
    return id;
  }

  public void lock(UUID id) {
    sql.one("SELECT id FROM app_user WHERE id=? FOR UPDATE", (r, n) -> r.getObject(1, UUID.class), id);
  }
}
