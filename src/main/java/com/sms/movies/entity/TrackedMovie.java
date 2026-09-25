package com.sms.movies.entity;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("movies")
public record TrackedMovie(
    @Id UUID id,
    String title,
    String description,
    @Column("poster_url") String posterUrl,
    String director,
    String genre,
    @Column("release_date") LocalDate releaseDate,
    @Column("duration_minutes") Integer durationMinutes,
    @Column("release_year") Integer releaseYear,
    @Column("external_id") String externalId) {}
