package com.sms.movies.service;

import com.sms.movies.dto.*;
import com.sms.movies.dto.CollectionDtos.*;
import com.sms.movies.exception.ApiException;
import com.sms.movies.repository.*;
import java.util.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CollectionService {
  static final RowMapper<CollectionResponse> MAPPER =
      (r, n) ->
          new CollectionResponse(
              r.getLong("id"),
              r.getString("name"),
              r.getTimestamp("created_at").toInstant(),
              r.getTimestamp("updated_at").toInstant());
  private final SqlRepository sql;
  private final MovieService movies;
  private final AuditService audit;
  private final AnalyticsService analytics;

  public CollectionService(
      SqlRepository sql, MovieService movies, AuditService audit, AnalyticsService analytics) {
    this.sql = sql;
    this.movies = movies;
    this.audit = audit;
    this.analytics = analytics;
  }

  public CollectionResponse owned(long user, long id) {
    var result =
        sql.one(
            "SELECT * FROM user_collection WHERE id=? AND user_id=? FOR UPDATE", MAPPER, id, user);
    if (result == null) throw ApiException.notFound();
    return result;
  }

  public PageResponse<CollectionResponse> list(long user, int page, int size) {
    return SqlRepository.page(
        sql.list(
            "SELECT * FROM user_collection WHERE user_id=? ORDER BY id LIMIT ? OFFSET ?",
            MAPPER,
            user,
            size,
            (long) page * size),
        page,
        size,
        sql.count("SELECT COUNT(*) FROM user_collection WHERE user_id=?", user));
  }

  public CollectionResponse create(long user, String name) {
    long id =
        sql.insert("INSERT INTO user_collection(user_id,name) VALUES(?,?)", user, name.strip());
    audit.record(user, "COLLECTION_CREATED", "COLLECTION", id, "SUCCESS");
    return owned(user, id);
  }

  public CollectionDetails get(long user, long id, int page, int size) {
    return new CollectionDetails(owned(user, id), items(id, page, size));
  }

  public PageResponse<MovieResponse> items(long id, int page, int size) {
    var ids =
        sql.list(
            "SELECT movie_id FROM collection_item WHERE collection_id=? ORDER BY movie_id LIMIT ?"
                + " OFFSET ?",
            (r, n) -> r.getLong(1),
            id,
            size,
            (long) page * size);
    return SqlRepository.page(
        ids.stream().map(movies::get).toList(),
        page,
        size,
        sql.count("SELECT COUNT(*) FROM collection_item WHERE collection_id=?", id));
  }

  public CollectionResponse rename(long user, long id, String name) {
    owned(user, id);
    sql.update(
        "UPDATE user_collection SET name=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
        name.strip(),
        id);
    audit.record(user, "COLLECTION_RENAMED", "COLLECTION", id, "SUCCESS");
    return owned(user, id);
  }

  public void delete(long user, long id) {
    owned(user, id);
    sql.update("DELETE FROM user_collection WHERE id=?", id);
    audit.record(user, "COLLECTION_DELETED", "COLLECTION", id, "SUCCESS");
  }

  public void add(long user, long id, long movie) {
    owned(user, id);
    movies.get(movie);
    if (sql.count(
            "SELECT COUNT(*) FROM collection_item WHERE collection_id=? AND movie_id=?", id, movie)
        == 0) {
      sql.update("INSERT INTO collection_item(collection_id,movie_id) VALUES(?,?)", id, movie);
      sql.update("UPDATE user_collection SET updated_at=CURRENT_TIMESTAMP WHERE id=?", id);
      audit.record(user, "COLLECTION_ITEM_ADDED", "COLLECTION_ITEM", id + ":" + movie, "SUCCESS");
      analytics.record(user, "collection_item_added", movie);
    }
  }

  public void remove(long user, long id, long movie) {
    owned(user, id);
    if (sql.update("DELETE FROM collection_item WHERE collection_id=? AND movie_id=?", id, movie)
        > 0) {
      sql.update("UPDATE user_collection SET updated_at=CURRENT_TIMESTAMP WHERE id=?", id);
      audit.record(user, "COLLECTION_ITEM_REMOVED", "COLLECTION_ITEM", id + ":" + movie, "SUCCESS");
      analytics.record(user, "collection_item_removed", movie);
    }
  }
}
