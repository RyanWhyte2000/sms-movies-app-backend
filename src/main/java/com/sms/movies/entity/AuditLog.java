package com.sms.movies.entity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("audit_log")
public record AuditLog(
    @Id UUID id,
    @Column("user_id") UUID userId,
    String action,
    String resource,
    @Column("occurred_at") Instant occurredAt,
    @Column("correlation_id") String correlationId) {}
