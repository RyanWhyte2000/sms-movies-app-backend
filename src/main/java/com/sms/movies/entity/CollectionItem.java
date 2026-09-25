package com.sms.movies.entity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("collection_items")
public record CollectionItem(
    @Id UUID id,
    @Column("user_id") UUID userId,
    @Column("movie_id") UUID movieId,
    @Column("added_at") Instant addedAt) {}
