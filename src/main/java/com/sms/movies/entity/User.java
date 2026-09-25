package com.sms.movies.entity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
public record User(
    @Id UUID id,
    String email,
    @Column("password_hash") String passwordHash,
    @Column("created_at") Instant createdAt) {}
