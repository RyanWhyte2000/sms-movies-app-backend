package com.sms.movies.repository.tracker;

import com.sms.movies.entity.CollectionItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionItemRepository extends ListCrudRepository<CollectionItem, UUID> {
  List<CollectionItem> findAllByUserId(UUID userId);

  Optional<CollectionItem> findByUserIdAndMovieId(UUID userId, UUID movieId);
}
