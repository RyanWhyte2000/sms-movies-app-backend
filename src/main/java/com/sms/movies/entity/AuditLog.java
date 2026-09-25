package com.sms.movies.entity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("audit_event")
public record AuditLog(
    @Id UUID id,
    @Column("actor_user_id") UUID userId,
    String action,
    @Column("resource_type") String resourceType,
    @Column("resource_id") String resourceId,
    String outcome,
    @Column("occurred_at") Instant occurredAt,
    @Column("correlation_id") String correlationId) {}
