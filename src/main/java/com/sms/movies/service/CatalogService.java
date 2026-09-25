package com.sms.movies.service;

import com.sms.movies.dto.*;
import com.sms.movies.dto.DiscoveryDtos.*;
import com.sms.movies.exception.*;
import com.sms.movies.repository.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogService {
  private final SqlRepository sql;
  private final MovieRepository movies;
  private final ActivityReporter activity;

  public CatalogService(
      SqlRepository sql, MovieRepository movies, ActivityReporter activity) {
    this.sql = sql;
    this.movies = movies;
    this.activity = activity;
  }

  public PageResponse<MovieResponse> search(
      UUID user, String q, UUID cast, UUID director, UUID genre, int page, int size) {
    var args = new ArrayList<Object>();
    String where = " WHERE 1=1";
    if (q != null && !q.isBlank()) {
      String like =
          "%"
              + q.strip()
                  .toLowerCase(Locale.ROOT)
                  .replace("!", "!!")
                  .replace("%", "!%")
                  .replace("_", "!_")
              + "%";
      where +=
          " AND (LOWER(m.\"TITLE\") LIKE ? ESCAPE '!' OR LOWER(m.\"DESCRIPTION\") LIKE ? ESCAPE '!'"
              + " OR EXISTS (SELECT 1 FROM movie_credit mc JOIN person p ON p.id=mc.person_id WHERE"
              + " mc.movie_id=m.\"ID\" AND LOWER(p.name) LIKE ? ESCAPE '!') OR EXISTS (SELECT 1"
              + " FROM movie_genre mg JOIN genre g ON g.id=mg.genre_id WHERE mg.movie_id=m.\"ID\""
              + " AND LOWER(g.name) LIKE ? ESCAPE '!'))";
      args.addAll(List.of(like, like, like, like));
    }
    if (cast != null) {
      where +=
          " AND EXISTS(SELECT 1 FROM movie_credit c WHERE c.movie_id=m.\"ID\" AND c.person_id=? AND"
              + " c.role='CAST')";
      args.add(cast);
    }
    if (director != null) {
      where +=
          " AND EXISTS(SELECT 1 FROM movie_credit c WHERE c.movie_id=m.\"ID\" AND c.person_id=? AND"
              + " c.role='DIRECTOR')";
      args.add(director);
    }
    if (genre != null) {
      where +=
          " AND EXISTS(SELECT 1 FROM movie_genre g WHERE g.movie_id=m.\"ID\" AND g.genre_id=?)";
      args.add(genre);
    }
    long total = sql.count("SELECT COUNT(*) FROM \"movies\" m" + where, args.toArray());
    args.add(size);
    args.add((long) page * size);
    var ids =
        sql.list(
            "SELECT m.\"ID\" FROM \"movies\" m" + where + " ORDER BY m.\"ID\" LIMIT ? OFFSET ?",
            (r, n) -> r.getObject(1, UUID.class),
            args.toArray());
    activity.audit(user, "SEARCH", "MOVIE", null, "SUCCESS");
    activity.analytics(user, "search_completed", null);
    return SqlRepository.page(
        ids.stream().map(id -> MovieResponse.from(movies.findById(id).orElseThrow())).toList(),
        page,
        size,
        total);
  }

  public MovieDetails details(UUID id, UUID user) {
    var m = movies.findById(id).orElseThrow(() -> new MovieNotFoundException(id));
    var rating =
        sql.one(
            "SELECT score FROM personal_rating WHERE user_id=? AND movie_id=?",
            (r, n) -> r.getInt(1),
            user,
            id);
    return new MovieDetails(
        m.id(),
        m.title(),
        m.description(),
        m.director(),
        m.genre(),
        m.releaseDate(),
        m.durationMinutes(),
        credits(id),
        movieGenres(id),
        externalRatings(id),
        rating);
  }

  public List<NamedItem> genres() {
    return sql.list(
        "SELECT id,name FROM genre ORDER BY name,id",
        (r, n) -> new NamedItem(r.getObject(1, UUID.class), r.getString(2)));
  }

  public PageResponse<NamedItem> people(String query, String role, int page, int size) {
    if (role != null && !Set.of("CAST", "DIRECTOR").contains(role))
      throw new ApiException(HttpStatus.BAD_REQUEST, "role must be CAST or DIRECTOR.");
    var args = new ArrayList<Object>();
    String where = " WHERE LOWER(p.name) LIKE ? ESCAPE '!'";
    args.add(
        "%"
            + query
                .toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
            + "%");
    if (role != null) {
      where += " AND EXISTS(SELECT 1 FROM movie_credit c WHERE c.person_id=p.id AND c.role=?)";
      args.add(role);
    }
    long total = sql.count("SELECT COUNT(*) FROM person p" + where, args.toArray());
    args.add(size);
    args.add((long) page * size);
    return SqlRepository.page(
        sql.list(
            "SELECT p.id,p.name FROM person p" + where + " ORDER BY p.name,p.id LIMIT ? OFFSET ?",
            (r, n) -> new NamedItem(r.getObject(1, UUID.class), r.getString(2)),
            args.toArray()),
        page,
        size,
        total);
  }

  public NamedItem createPerson(String name) {
    UUID id = UUID.randomUUID();
    sql.update("INSERT INTO person(id,name) VALUES(?,?)", id, name.strip());
    return new NamedItem(id, name.strip());
  }

  public NamedItem createGenre(String name) {
    UUID id = UUID.randomUUID();
    sql.update("INSERT INTO genre(id,name) VALUES(?,?)", id, name.strip());
    return new NamedItem(id, name.strip());
  }

  public List<Credit> credits(UUID movie) {
    return sql.list(
        "SELECT p.id,p.name,c.role FROM movie_credit c JOIN person p ON p.id=c.person_id WHERE"
            + " c.movie_id=? ORDER BY c.role,p.name,p.id",
        (r, n) -> new Credit(r.getObject(1, UUID.class), r.getString(2), r.getString(3)),
        movie);
  }

  public List<NamedItem> movieGenres(UUID movie) {
    return sql.list(
        "SELECT g.id,g.name FROM movie_genre mg JOIN genre g ON g.id=mg.genre_id WHERE"
            + " mg.movie_id=? ORDER BY g.name,g.id",
        (r, n) -> new NamedItem(r.getObject(1, UUID.class), r.getString(2)),
        movie);
  }

  public List<ExternalRating> externalRatings(UUID movie) {
    return sql.list(
        "SELECT * FROM external_rating WHERE movie_id=? ORDER BY source",
        (r, n) ->
            new ExternalRating(
                r.getString("source"),
                r.getDouble("score"),
                r.getDouble("scale_max"),
                (Long) r.getObject("vote_count"),
                r.getTimestamp("fetched_at").toInstant()),
        movie);
  }

  public void sync(UUID movie, MovieRequest request) {
    sql.update("DELETE FROM movie_credit WHERE movie_id=?", movie);
    sql.update("DELETE FROM movie_genre WHERE movie_id=?", movie);
    // Legacy single director/genre fields remain convenient inputs; IDs distinguish namesakes.
    UUID director =
        sql.one(
            "SELECT id FROM person WHERE name=? ORDER BY id LIMIT 1",
            (r, n) -> r.getObject(1, UUID.class),
            request.director().strip());
    if (director == null) director = createPerson(request.director()).id();
    UUID genre =
        sql.one(
            "SELECT id FROM genre WHERE name=?",
            (r, n) -> r.getObject(1, UUID.class),
            request.genre().strip());
    if (genre == null) genre = createGenre(request.genre()).id();
    Set<UUID> directors = new HashSet<>();
    directors.add(director);
    if (request.directorIds() != null) directors.addAll(request.directorIds());
    Set<UUID> genres = new HashSet<>();
    genres.add(genre);
    if (request.genreIds() != null) genres.addAll(request.genreIds());
    for (UUID id : directors)
      sql.update(
          "INSERT INTO movie_credit(movie_id,person_id,role) VALUES(?,?,'DIRECTOR')", movie, id);
    if (request.castIds() != null)
      for (UUID id : new HashSet<>(request.castIds()))
        sql.update(
            "INSERT INTO movie_credit(movie_id,person_id,role) VALUES(?,?,'CAST')", movie, id);
    for (UUID id : genres)
      sql.update("INSERT INTO movie_genre(movie_id,genre_id) VALUES(?,?)", movie, id);
  }

  public ExternalRating putExternal(
      UUID user, UUID movie, String source, ExternalRatingRequest request) {
    if (request.score() > request.scaleMax()
        || !Double.isFinite(request.score())
        || !Double.isFinite(request.scaleMax()))
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "Rating must be finite and within its source scale.");
    if (sql.one(
            "SELECT \"ID\" FROM \"movies\" WHERE \"ID\"=? FOR UPDATE",
            (r, n) -> r.getObject(1, UUID.class),
            movie)
        == null) throw new MovieNotFoundException(movie);
    int updated =
        sql.update(
            "UPDATE external_rating SET"
                + " score=?,scale_max=?,vote_count=?,fetched_at=CURRENT_TIMESTAMP WHERE movie_id=?"
                + " AND source=?",
            request.score(),
            request.scaleMax(),
            request.voteCount(),
            movie,
            source);
    if (updated == 0)
      sql.update(
          "INSERT INTO external_rating(movie_id,source,score,scale_max,vote_count)"
              + " VALUES(?,?,?,?,?)",
          movie,
          source,
          request.score(),
          request.scaleMax(),
          request.voteCount());
    activity.audit(user, "EXTERNAL_RATING_UPDATED", "MOVIE", movie, "SUCCESS");
    return externalRatings(movie).stream()
        .filter(r -> r.source().equals(source))
        .findFirst()
        .orElseThrow();
  }
}
