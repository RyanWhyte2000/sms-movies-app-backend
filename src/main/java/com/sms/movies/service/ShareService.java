package com.sms.movies.service;

import com.sms.movies.dto.CollectionDtos.*;
import com.sms.movies.exception.ApiException;
import com.sms.movies.repository.SqlRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShareService {
  private final SqlRepository sql;
  private final CollectionService collections;
  private final ActivityReporter activity;
  private final ShareTokenCodec tokens;

  public ShareService(
      SqlRepository sql,
      CollectionService collections,
      ActivityReporter activity,
      ShareTokenCodec tokens) {
    this.sql = sql;
    this.collections = collections;
    this.activity = activity;
    this.tokens = tokens;
  }

  public CreatedShare create(UUID user, UUID collection, Instant expires) {
    collections.owned(user, collection);
    String token = tokens.generate();
    UUID id = UUID.randomUUID();
    sql.update(
        "INSERT INTO collection_share(id,collection_id,token_hash,expires_at) VALUES(?,?,?,?)",
        id,
        collection,
        tokens.digest(token),
        expires == null ? null : expires.atOffset(ZoneOffset.UTC));
    activity.audit(user, "SHARE_CREATED", "COLLECTION", collection, "SUCCESS");
    return new CreatedShare(id, token, "/api/shared/collections/" + token, expires);
  }

  public List<ShareResponse> list(UUID user, UUID collection) {
    collections.owned(user, collection);
    return sql.list(
        "SELECT * FROM collection_share WHERE collection_id=? AND revoked_at IS NULL AND"
            + " (expires_at IS NULL OR expires_at>CURRENT_TIMESTAMP) ORDER BY id",
        (r, n) ->
            new ShareResponse(
                r.getObject("id", UUID.class),
                r.getTimestamp("created_at").toInstant(),
                r.getTimestamp("expires_at") == null
                    ? null
                    : r.getTimestamp("expires_at").toInstant(),
                null),
        collection);
  }

  public void revoke(UUID user, UUID collection, UUID id) {
    collections.owned(user, collection);
    if (sql.count(
            "SELECT COUNT(*) FROM collection_share WHERE id=? AND collection_id=?", id, collection)
        == 0) throw ApiException.notFound();
    if (sql.update(
            "UPDATE collection_share SET revoked_at=CURRENT_TIMESTAMP WHERE id=? AND"
                + " collection_id=? AND revoked_at IS NULL",
            id,
            collection)
        > 0) activity.audit(user, "SHARE_REVOKED", "COLLECTION", collection, "SUCCESS");
  }

  @Transactional(readOnly = true)
  public SharedCollection read(String token, int page, int size) {
    if (!tokens.isValid(token)) throw ApiException.notFound();
    var share =
        sql.one(
            "SELECT c.id,c.name FROM collection_share s JOIN user_collection c ON"
                + " c.id=s.collection_id WHERE s.token_hash=? AND s.revoked_at IS NULL AND"
                + " (s.expires_at IS NULL OR s.expires_at>CURRENT_TIMESTAMP)",
            (r, n) -> Map.entry(r.getObject("id", UUID.class), r.getString("name")),
            tokens.digest(token));
    if (share == null) throw ApiException.notFound();
    return new SharedCollection(share.getValue(), collections.items(share.getKey(), page, size));
  }

}
