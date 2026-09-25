package com.sms.movies.entity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("idempotency_key")
public record IdempotencyKey(
    @Id UUID id,
    @Column("user_id") UUID userId,
    String key,
    @Column("result_json") String resultJson,
    @Column("created_at") Instant createdAt,
    @Column("expires_at") Instant expiresAt) {}
