package com.sms.movies.service;

import com.sms.movies.dto.*;
import com.sms.movies.dto.RatingDtos.*;
import com.sms.movies.repository.*;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RatingService {
  private static final RowMapper<RatingResponse> MAPPER =
      (r, n) ->
          new RatingResponse(
              r.getObject("movie_id", UUID.class),
              r.getInt("score"),
              r.getTimestamp("created_at").toInstant(),
              r.getTimestamp("updated_at").toInstant());
  private final SqlRepository sql;
  private final UserRepository users;
  private final MovieService movies;
  private final ActivityReporter activity;

  public RatingService(
      SqlRepository sql,
      UserRepository users,
      MovieService movies,
      ActivityReporter activity) {
    this.sql = sql;
    this.users = users;
    this.movies = movies;
    this.activity = activity;
  }

  public RatingResponse put(UUID user, UUID movie, int score) {
    users.lock(user);
    movies.get(movie);
    var existing =
        sql.one(
            "SELECT * FROM personal_rating WHERE user_id=? AND movie_id=?", MAPPER, user, movie);
    if (existing != null && existing.score() == score) return existing;
    if (existing == null)
      sql.update(
          "INSERT INTO personal_rating(user_id,movie_id,score) VALUES(?,?,?)", user, movie, score);
    else
      sql.update(
          "UPDATE personal_rating SET score=?,updated_at=CURRENT_TIMESTAMP WHERE user_id=? AND"
              + " movie_id=?",
          score,
          user,
          movie);
    activity.audit(user, "RATING_SAVED", "MOVIE", movie, "SUCCESS");
    activity.analytics(user, "rating_saved", movie);
    return sql.one(
        "SELECT * FROM personal_rating WHERE user_id=? AND movie_id=?", MAPPER, user, movie);
  }

  public void delete(UUID user, UUID movie) {
    users.lock(user);
    if (sql.update("DELETE FROM personal_rating WHERE user_id=? AND movie_id=?", user, movie) > 0) {
      activity.audit(user, "RATING_REMOVED", "MOVIE", movie, "SUCCESS");
      activity.analytics(user, "rating_removed", movie);
    }
  }

  @Transactional(readOnly = true)
  public PageResponse<RatingResponse> list(UUID user, int page, int size) {
    return SqlRepository.page(
        sql.list(
            "SELECT * FROM personal_rating WHERE user_id=? ORDER BY movie_id LIMIT ? OFFSET ?",
            MAPPER,
            user,
            size,
            (long) page * size),
        page,
        size,
        sql.count("SELECT COUNT(*) FROM personal_rating WHERE user_id=?", user));
  }
}
