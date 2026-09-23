package com.sms.movies.repository;

import com.sms.movies.dto.PageResponse;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class SqlRepository {
  private final JdbcTemplate jdbc;

  public SqlRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public int update(String sql, Object... args) {
    return jdbc.update(sql, args);
  }

  public <T> List<T> list(String sql, RowMapper<T> mapper, Object... args) {
    return jdbc.query(sql, mapper, args);
  }

  public <T> T one(String sql, RowMapper<T> mapper, Object... args) {
    return list(sql, mapper, args).stream().findFirst().orElse(null);
  }

  public long count(String sql, Object... args) {
    return jdbc.queryForObject(sql, Long.class, args);
  }

  public long insert(String sql, Object... args) {
    var key = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          var statement = connection.prepareStatement(sql, new String[] {"ID"});
          for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
          return statement;
        },
        key);
    return key.getKey().longValue();
  }

  public static <T> PageResponse<T> page(List<T> content, int page, int size, long total) {
    return new PageResponse<>(content, page, size, total, (int) ((total + size - 1) / size));
  }
}
