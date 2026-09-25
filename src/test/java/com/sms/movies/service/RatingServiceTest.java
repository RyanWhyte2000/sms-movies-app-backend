package com.sms.movies.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sms.movies.dto.RatingDtos.RatingResponse;
import com.sms.movies.repository.SqlRepository;
import com.sms.movies.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;

class RatingServiceTest {
  @Test
  void submittingAnotherRatingUpdatesTheUsersExistingMovieRating() {
    var sql = mock(SqlRepository.class);
    var users = mock(UserRepository.class);
    var movies = mock(MovieService.class);
    var activity = mock(ActivityReporter.class);
    var service = new RatingService(sql, users, movies, activity);
    var user = UUID.randomUUID();
    var movie = UUID.randomUUID();
    var existing = new RatingResponse(movie, 2, Instant.now(), Instant.now());
    var updated = new RatingResponse(movie, 5, existing.createdAt(), Instant.now());

    when(sql.one(contains("personal_rating"), org.mockito.ArgumentMatchers.<RowMapper<RatingResponse>>any(), eq(user), eq(movie)))
        .thenReturn(existing, updated);

    assertEquals(updated, service.put(user, movie, 5));

    verify(sql)
        .update(
            contains("UPDATE personal_rating"),
            eq(5),
            eq(user),
            eq(movie));
    verify(sql, never())
        .update(contains("INSERT INTO personal_rating"), eq(user), eq(movie), eq(5));
  }
}
