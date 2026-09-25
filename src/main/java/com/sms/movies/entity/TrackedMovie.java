package com.sms.movies.entity;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("movies")
public record TrackedMovie(
    @Id UUID id,
    String title,
    @Column("release_year") Integer releaseYear,
    @Column("external_id") String externalId) {}
