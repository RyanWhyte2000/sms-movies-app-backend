package com.sms.movies.repository.tracker;

import com.sms.movies.entity.MovieGenre;
import com.sms.movies.entity.MovieGenreId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieGenreRepository extends ListCrudRepository<MovieGenre, MovieGenreId> {
  List<MovieGenre> findAllByIdMovieId(UUID movieId);

  List<MovieGenre> findAllByIdGenreId(UUID genreId);
}
