package com.sms.movies.repository.tracker;

import com.sms.movies.entity.Person;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends ListCrudRepository<Person, UUID> {
  List<Person> findAllByNameIgnoreCase(String name);
}
