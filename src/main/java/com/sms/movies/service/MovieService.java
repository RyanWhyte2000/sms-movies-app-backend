package com.sms.movies.service;

import com.sms.movies.dto.MovieRequest;
import com.sms.movies.dto.MovieResponse;
import com.sms.movies.dto.PageResponse;
import com.sms.movies.exception.MovieNotFoundException;
import com.sms.movies.model.Movie;
import com.sms.movies.repository.MovieRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MovieService {
  private final MovieRepository repository;

  private final CatalogService catalog;

  public MovieService(MovieRepository repository, CatalogService catalog) {
    this.repository = repository;
    this.catalog = catalog;
  }

  public PageResponse<MovieResponse> list(int page, int size) {
    return PageResponse.from(
        repository.findAll(PageRequest.of(page, size, Sort.by("id"))).map(MovieResponse::from));
  }

  public MovieResponse get(long id) {
    return MovieResponse.from(find(id));
  }

  @Transactional
  public MovieResponse create(MovieRequest request) {
    var saved = repository.save(toMovie(null, request));
    catalog.sync(saved.id(), request);
    return MovieResponse.from(saved);
  }

  @Transactional
  public MovieResponse update(long id, MovieRequest request) {
    find(id);
    var saved = repository.save(toMovie(id, request));
    catalog.sync(id, request);
    return MovieResponse.from(saved);
  }

  @Transactional
  public void delete(long id) {
    repository.delete(find(id));
  }

  private Movie find(long id) {
    return repository.findById(id).orElseThrow(() -> new MovieNotFoundException(id));
  }

  private Movie toMovie(Long id, MovieRequest request) {
    return new Movie(
        id,
        request.title().strip(),
        request.description(),
        request.director().strip(),
        request.genre().strip(),
        request.releaseDate(),
        request.durationMinutes());
  }
}
