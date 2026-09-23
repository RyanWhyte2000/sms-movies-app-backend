package com.sms.movies.model;

import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("movies")
public record Movie(
    @Id Long id,
    String title,
    String description,
    String director,
    String genre,
    LocalDate releaseDate,
    Integer durationMinutes) {}
