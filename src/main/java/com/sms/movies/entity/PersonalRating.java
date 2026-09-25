package com.sms.movies.entity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("personal_rating")
public record PersonalRating(
    @Id UUID id,
    @Column("user_id") UUID userId,
    @Column("movie_id") UUID movieId,
    Integer score,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt) {}
