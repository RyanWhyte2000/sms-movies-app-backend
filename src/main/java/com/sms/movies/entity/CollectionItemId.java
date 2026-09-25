package com.sms.movies.entity;

import java.util.UUID;
import org.springframework.data.relational.core.mapping.Column;

public record CollectionItemId(
    @Column("collection_id") UUID collectionId, @Column("movie_id") UUID movieId) {}
