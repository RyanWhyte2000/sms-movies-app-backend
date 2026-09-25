package com.sms.movies.entity;

import java.util.UUID;
import org.springframework.data.relational.core.mapping.Column;

public record MovieCreditId(
    @Column("movie_id") UUID movieId,
    @Column("person_id") UUID personId,
    String role) {}
