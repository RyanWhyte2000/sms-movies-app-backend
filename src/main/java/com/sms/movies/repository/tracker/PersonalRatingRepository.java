package com.sms.movies.repository.tracker;

import com.sms.movies.entity.PersonalRating;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonalRatingRepository extends ListCrudRepository<PersonalRating, UUID> {
  List<PersonalRating> findAllByUserId(UUID userId);

  Optional<PersonalRating> findByUserIdAndMovieId(UUID userId, UUID movieId);

  @Modifying
  @Query(
      """
      INSERT INTO personal_ratings (id, user_id, movie_id, score, created_at)
      VALUES (:id, :userId, :movieId, :score, CURRENT_TIMESTAMP)
      ON CONFLICT (user_id, movie_id)
      DO UPDATE SET score = EXCLUDED.score
      """)
  int upsert(UUID id, UUID userId, UUID movieId, int score);
}
