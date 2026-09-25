package com.sms.movies.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

@Table("movie_genres")
public record MovieGenre(@Id @Embedded.Nullable MovieGenreId id) {}
