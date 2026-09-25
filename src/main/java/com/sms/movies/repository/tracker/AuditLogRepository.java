package com.sms.movies.repository.tracker;

import com.sms.movies.entity.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends ListCrudRepository<AuditLog, UUID> {
  List<AuditLog> findAllByUserId(UUID userId);

  List<AuditLog> findAllByCorrelationId(String correlationId);
}
