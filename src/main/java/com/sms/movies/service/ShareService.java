package com.sms.movies.service;

import com.sms.movies.dto.CollectionDtos.*;
import com.sms.movies.exception.ApiException;
import com.sms.movies.repository.SqlRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShareService {
  private final SqlRepository sql;
  private final CollectionService collections;
  private final AuditService audit;
  private final SecureRandom random = new SecureRandom();

  public ShareService(SqlRepository sql, CollectionService collections, AuditService audit) {
    this.sql = sql;
    this.collections = collections;
    this.audit = audit;
  }

  public CreatedShare create(long user, long collection, Instant expires) {
    collections.owned(user, collection);
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    long id =
        sql.insert(
            "INSERT INTO collection_share(collection_id,token_hash,expires_at) VALUES(?,?,?)",
            collection,
            hash(token),
            expires == null ? null : expires.atOffset(ZoneOffset.UTC));
    audit.record(user, "SHARE_CREATED", "COLLECTION", collection, "SUCCESS");
    return new CreatedShare(id, token, "/api/shared/collections/" + token, expires);
  }

  public List<ShareResponse> list(long user, long collection) {
    collections.owned(user, collection);
    return sql.list(
        "SELECT * FROM collection_share WHERE collection_id=? AND revoked_at IS NULL AND"
            + " (expires_at IS NULL OR expires_at>CURRENT_TIMESTAMP) ORDER BY id",
        (r, n) ->
            new ShareResponse(
                r.getLong("id"),
                r.getTimestamp("created_at").toInstant(),
                r.getTimestamp("expires_at") == null
                    ? null
                    : r.getTimestamp("expires_at").toInstant(),
                null),
        collection);
  }

  public void revoke(long user, long collection, long id) {
    collections.owned(user, collection);
    if (sql.count(
            "SELECT COUNT(*) FROM collection_share WHERE id=? AND collection_id=?", id, collection)
        == 0) throw ApiException.notFound();
    if (sql.update(
            "UPDATE collection_share SET revoked_at=CURRENT_TIMESTAMP WHERE id=? AND"
                + " collection_id=? AND revoked_at IS NULL",
            id,
            collection)
        > 0) audit.record(user, "SHARE_REVOKED", "COLLECTION", collection, "SUCCESS");
  }

  @Transactional(readOnly = true)
  public SharedCollection read(String token, int page, int size) {
    if (!token.matches("[A-Za-z0-9_-]{43}")) throw ApiException.notFound();
    var share =
        sql.one(
            "SELECT c.id,c.name FROM collection_share s JOIN user_collection c ON"
                + " c.id=s.collection_id WHERE s.token_hash=? AND s.revoked_at IS NULL AND"
                + " (s.expires_at IS NULL OR s.expires_at>CURRENT_TIMESTAMP)",
            (r, n) -> Map.entry(r.getLong("id"), r.getString("name")),
            hash(token));
    if (share == null) throw ApiException.notFound();
    return new SharedCollection(share.getValue(), collections.items(share.getKey(), page, size));
  }

  private String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
