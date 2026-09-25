package com.sms.movies.model;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("movies")
public record Movie(
    @Id UUID id,
    String title,
    String description,
    String director,
    String genre,
    LocalDate releaseDate,
    Integer durationMinutes) {}
