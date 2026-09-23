package com.sms.movies.repository;

import com.sms.movies.model.Movie;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;

public interface MovieRepository
    extends ListCrudRepository<Movie, Long>, ListPagingAndSortingRepository<Movie, Long> {}
