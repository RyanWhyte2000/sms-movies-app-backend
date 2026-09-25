package com.sms.movies.entity;

import java.util.UUID;
import org.springframework.data.relational.core.mapping.Column;

public record MovieGenreId(
    @Column("movie_id") UUID movieId, @Column("genre_id") UUID genreId) {}
