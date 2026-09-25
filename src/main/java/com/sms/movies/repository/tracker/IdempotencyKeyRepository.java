package com.sms.movies.repository.tracker;

import com.sms.movies.entity.IdempotencyKey;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface IdempotencyKeyRepository extends ListCrudRepository<IdempotencyKey, UUID> {
  Optional<IdempotencyKey> findByKey(String key);

  Optional<IdempotencyKey> findByUserIdAndKey(UUID userId, String key);
}
