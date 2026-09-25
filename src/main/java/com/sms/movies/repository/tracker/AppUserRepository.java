package com.sms.movies.repository.tracker;

import com.sms.movies.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface AppUserRepository extends ListCrudRepository<User, UUID> {
  Optional<User> findByEmail(String email);
}
