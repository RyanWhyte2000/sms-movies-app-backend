package com.sms.movies.entity;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

@Table("collection_item")
public record CollectionItem(
    @Id @Embedded.Nullable CollectionItemId id, @Column("added_at") Instant addedAt) {}
