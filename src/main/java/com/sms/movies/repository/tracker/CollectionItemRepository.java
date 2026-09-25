package com.sms.movies.repository.tracker;

import com.sms.movies.entity.CollectionItem;
import com.sms.movies.entity.CollectionItemId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionItemRepository
    extends ListCrudRepository<CollectionItem, CollectionItemId> {
  List<CollectionItem> findAllByIdCollectionId(UUID collectionId);

  Optional<CollectionItem> findByIdCollectionIdAndIdMovieId(UUID collectionId, UUID movieId);
}
