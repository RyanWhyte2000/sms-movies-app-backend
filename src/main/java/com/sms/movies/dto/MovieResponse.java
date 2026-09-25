package com.sms.movies.dto;

import com.sms.movies.model.Movie;
import java.time.LocalDate;
import java.util.UUID;

public record MovieResponse(
    UUID id,
    String title,
    String description,
    String director,
    String genre,
    LocalDate releaseDate,
    Integer durationMinutes) {
  public static MovieResponse from(Movie movie) {
    return new MovieResponse(
        movie.id(),
        movie.title(),
        movie.description(),
        movie.director(),
        movie.genre(),
        movie.releaseDate(),
        movie.durationMinutes());
  }
}
