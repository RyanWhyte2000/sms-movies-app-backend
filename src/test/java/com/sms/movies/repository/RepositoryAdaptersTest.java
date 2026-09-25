package com.sms.movies.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.*;

class RepositoryAdaptersTest {
  @Test
  void sqlRepositoryDelegatesAndBuildsPages() {
    var jdbc = mock(JdbcTemplate.class);
    var sql = new SqlRepository(jdbc);
    when(jdbc.update(anyString(), any(Object[].class))).thenReturn(2);
    when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<String>>any(), any(Object[].class)))
        .thenReturn(List.of("one", "two"));
    when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(7L);

    assertEquals(2, sql.update("update", 1));
    assertEquals(List.of("one", "two"), sql.list("select", (r, n) -> "x", 1));
    assertEquals("one", sql.one("select", (r, n) -> "x", 1));
    assertEquals(7, sql.count("count", 1));
    var page = SqlRepository.page(List.of("x"), 1, 3, 7);
    assertEquals(3, page.totalPages());
  }

  @Test
  void userRepositoryMapsCreatesAndLocks() throws Exception {
    var sql = mock(SqlRepository.class);
    var repository = new UserRepository(sql);
    var id = UUID.randomUUID();
    when(sql.one(startsWith("SELECT *"), any(), any(Object[].class))).thenAnswer(invocation -> {
      var row = mock(java.sql.ResultSet.class);
      when(row.getObject("id", UUID.class)).thenReturn(id);
      when(row.getString("email")).thenReturn("user@example.com");
      when(row.getString("display_name")).thenReturn("User");
      when(row.getTimestamp("created_at")).thenReturn(Timestamp.from(Instant.now()));
      when(row.getString("password_hash")).thenReturn("hash");
      when(row.getString("role")).thenReturn("USER");
      return invocation.<RowMapper<UserRepository.Account>>getArgument(1).mapRow(row, 0);
    });
    assertEquals(id, repository.find("user@example.com").user().id());
    assertNotNull(repository.create("new@example.com", "hash", "New"));
    repository.lock(id);
    verify(sql).update(startsWith("INSERT INTO app_user"), any(Object[].class));
    verify(sql).one(startsWith("SELECT id"), any(), any(Object[].class));
  }
}
