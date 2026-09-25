package com.sms.movies.repository.tracker;

import com.sms.movies.entity.Genre;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface GenreRepository extends ListCrudRepository<Genre, UUID> {
  Optional<Genre> findByName(String name);
}
