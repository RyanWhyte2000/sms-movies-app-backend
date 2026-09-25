package com.sms.movies.service;

import com.sms.movies.dto.AnalyticsDtos.EventRequest;
import com.sms.movies.exception.ApiException;
import com.sms.movies.repository.*;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
  private final SqlRepository sql;
  private final UserRepository users;

  public AnalyticsService(SqlRepository sql, UserRepository users) {
    this.sql = sql;
    this.users = users;
  }

  public void record(UUID user, String event, UUID movie) {
    sql.update(
        "INSERT INTO analytics_event(event_id,user_id,event_name,movie_id) VALUES(?,?,?,?)",
        UUID.randomUUID().toString(),
        user,
        event,
        movie);
  }

  @Transactional
  public void accept(UUID user, EventRequest event) {
    users.lock(user);
    var existing =
        sql.one(
            "SELECT user_id,event_name,movie_id,journey_id FROM analytics_event WHERE event_id=?",
            (r, n) ->
                new Existing(
                    r.getObject("user_id", UUID.class),
                    r.getString("event_name"),
                    r.getObject("movie_id", UUID.class),
                    r.getString("journey_id")),
            event.eventId().toString());
    String name = event.eventName().name().toLowerCase(Locale.ROOT);
    String journey = event.journeyId() == null ? null : event.journeyId().toString();
    if (existing != null) {
      if (!existing.user().equals(user)
          || !existing.name().equals(name)
          || !existing.movie().equals(event.movieId())
          || !java.util.Objects.equals(existing.journey(), journey))
        throw new ApiException(
            org.springframework.http.HttpStatus.CONFLICT, "Event ID is already in use.");
      return;
    }
    if (sql.count("SELECT COUNT(*) FROM movies WHERE id=?", event.movieId()) == 0)
      throw ApiException.notFound();
    sql.update(
        "INSERT INTO analytics_event(event_id,user_id,event_name,movie_id,journey_id)"
            + " VALUES(?,?,?,?,?)",
        event.eventId().toString(),
        user,
        name,
        event.movieId(),
        journey);
  }

  private record Existing(UUID user, String name, UUID movie, String journey) {}
}
