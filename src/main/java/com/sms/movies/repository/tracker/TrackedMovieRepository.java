package com.sms.movies.repository.tracker;

import com.sms.movies.entity.TrackedMovie;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TrackedMovieRepository
    extends ListCrudRepository<TrackedMovie, UUID>,
        ListPagingAndSortingRepository<TrackedMovie, UUID> {
  Optional<TrackedMovie> findByExternalId(String externalId);
}
