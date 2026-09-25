package com.sms.movies.service;

import com.sms.movies.dto.PageResponse;
import com.sms.movies.repository.SqlRepository;
import java.time.Instant;
import java.util.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
  public record AuditEvent(
      UUID id,
      String action,
      String resourceType,
      String resourceId,
      String outcome,
      Instant occurredAt,
      String correlationId) {}

  private final SqlRepository sql;

  public AuditService(SqlRepository sql) {
    this.sql = sql;
  }

  public static String correlationId() {
    return Optional.ofNullable(MDC.get("correlationId"))
        .orElseGet(() -> UUID.randomUUID().toString());
  }

  public void record(UUID user, String action, String type, Object resource, String outcome) {
    sql.update(
        "INSERT INTO"
            + " audit_event(actor_user_id,action,resource_type,resource_id,outcome,correlation_id)"
            + " VALUES(?,?,?,?,?,?)",
        user,
        action,
        type,
        resource == null ? null : resource.toString(),
        outcome,
        correlationId());
  }

  @Transactional(readOnly = true)
  public PageResponse<AuditEvent> list(
      UUID user, String action, Instant from, Instant to, int page, int size) {
    var args = new ArrayList<Object>();
    args.add(user);
    String where = " WHERE actor_user_id=?";
    if (action != null && !action.isBlank()) {
      where += " AND action=?";
      args.add(action);
    }
    if (from != null) {
      where += " AND occurred_at>=?";
      args.add(from.atOffset(java.time.ZoneOffset.UTC));
    }
    if (to != null) {
      where += " AND occurred_at<=?";
      args.add(to.atOffset(java.time.ZoneOffset.UTC));
    }
    long count = sql.count("SELECT COUNT(*) FROM audit_event" + where, args.toArray());
    args.add(size);
    args.add((long) page * size);
    var records =
        sql.list(
            "SELECT * FROM audit_event" + where + " ORDER BY id DESC LIMIT ? OFFSET ?",
            (r, n) ->
                new AuditEvent(
                    r.getObject("id", UUID.class),
                    r.getString("action"),
                    r.getString("resource_type"),
                    r.getString("resource_id"),
                    r.getString("outcome"),
                    r.getTimestamp("occurred_at").toInstant(),
                    r.getString("correlation_id")),
            args.toArray());
    return SqlRepository.page(records, page, size, count);
  }
}
