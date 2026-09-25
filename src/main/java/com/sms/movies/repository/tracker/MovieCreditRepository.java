package com.sms.movies.repository.tracker;

import com.sms.movies.entity.MovieCredit;
import com.sms.movies.entity.MovieCreditId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface MovieCreditRepository extends ListCrudRepository<MovieCredit, MovieCreditId> {
  List<MovieCredit> findAllByIdMovieId(UUID movieId);

  List<MovieCredit> findAllByIdPersonId(UUID personId);
}
