package com.sms.movies;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sms.movies.dto.AccountDtos.*;
import com.sms.movies.dto.MovieRequest;
import com.sms.movies.repository.SqlRepository;
import com.sms.movies.service.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class FrontendApiTests {
  @Autowired MockMvc mvc;
  @Autowired AccountService accounts;
  @Autowired MovieService movies;
  @Autowired CatalogService catalogue;
  @Autowired SqlRepository sql;
  long alice, bob, movie, otherMovie, collection, actor, genre;

  @BeforeEach
  void fixtures() {
    alice =
        accounts
            .register(new RegisterRequest("alice@example.test", "a-long-test-password", "Alice"))
            .id();
    bob =
        accounts
            .register(new RegisterRequest("bob@example.test", "a-long-test-password", "Bob"))
            .id();
    actor = catalogue.createPerson("Example Actor").id();
    movie =
        movies
            .create(
                new MovieRequest(
                    "Space Laughs",
                    "A funny comedy about astronauts in space.",
                    "Director One",
                    "Comedy",
                    LocalDate.of(2020, 1, 1),
                    100,
                    List.of(actor),
                    null,
                    null))
            .id();
    otherMovie =
        movies
            .create(
                new MovieRequest(
                    "Space Friends",
                    "A comedy about friendship in space.",
                    "Director Two",
                    "Comedy",
                    LocalDate.of(2021, 1, 1),
                    110,
                    null,
                    null,
                    null))
            .id();
    collection =
        sql.one("SELECT id FROM user_collection WHERE user_id=?", (r, n) -> r.getLong(1), alice);
    genre = sql.one("SELECT id FROM genre WHERE name='Comedy'", (r, n) -> r.getLong(1));
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor alice() {
    return user("alice@example.test");
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor bob() {
    return user("bob@example.test");
  }

  private String collectionPath() {
    return "/api/me/collections/" + collection;
  }

  @Test
  void returnsCurrentUserWithoutSecrets() throws Exception {
    mvc.perform(get("/api/me").with(alice()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("alice@example.test"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void collectionOwnershipAndIdempotency() throws Exception {
    mvc.perform(get(collectionPath()).with(bob())).andExpect(status().isNotFound());
    mvc.perform(
            patch(collectionPath())
                .with(bob())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Stolen\"}"))
        .andExpect(status().isNotFound());
    for (int i = 0; i < 2; i++)
      mvc.perform(put(collectionPath() + "/movies/" + movie).with(alice()).with(csrf()))
          .andExpect(status().isNoContent());
    assertThat(sql.count("SELECT COUNT(*) FROM collection_item WHERE collection_id=?", collection))
        .isEqualTo(1);
    assertThat(
            sql.count(
                "SELECT COUNT(*) FROM audit_event WHERE actor_user_id=? AND"
                    + " action='COLLECTION_ITEM_ADDED'",
                alice))
        .isEqualTo(1);
    mvc.perform(get(collectionPath()).with(alice()))
        .andExpect(jsonPath("$.movies.content[0].id").value(movie));
    mvc.perform(delete(collectionPath() + "/movies/" + movie).with(bob()).with(csrf()))
        .andExpect(status().isNotFound());
    mvc.perform(delete(collectionPath() + "/movies/" + movie).with(alice()).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(delete(collectionPath() + "/movies/" + movie).with(alice()).with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  void createsRenamesAndDeletesCollection() throws Exception {
    var response =
        mvc.perform(
                post("/api/me/collections")
                    .with(alice())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Favorites\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    String location = response.getHeader("Location");
    mvc.perform(
            patch(location)
                .with(alice())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Weekend\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Weekend"));
    mvc.perform(get("/api/me/collections?size=1").with(alice()))
        .andExpect(jsonPath("$.totalElements").value(2));
    mvc.perform(delete(location).with(alice()).with(csrf())).andExpect(status().isNoContent());
    mvc.perform(get(location).with(alice())).andExpect(status().isNotFound());
  }

  @Test
  void ratingsAreUniqueAndPrivate() throws Exception {
    String path = "/api/me/ratings/" + movie;
    for (int score : new int[] {4, 5, 5})
      mvc.perform(
              put(path)
                  .with(alice())
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"score\":" + score + "}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.score").value(score));
    assertThat(
            sql.count(
                "SELECT COUNT(*) FROM personal_rating WHERE user_id=? AND movie_id=?",
                alice,
                movie))
        .isEqualTo(1);
    assertThat(
            sql.count(
                "SELECT COUNT(*) FROM audit_event WHERE actor_user_id=? AND action='RATING_SAVED'",
                alice))
        .isEqualTo(2);
    mvc.perform(get("/api/me/ratings").with(bob())).andExpect(jsonPath("$.totalElements").value(0));
    mvc.perform(get("/api/movies/" + movie).with(alice()))
        .andExpect(jsonPath("$.personalRating").value(5));
    mvc.perform(delete(path).with(bob()).with(csrf())).andExpect(status().isNoContent());
    assertThat(sql.count("SELECT COUNT(*) FROM personal_rating WHERE user_id=?", alice))
        .isEqualTo(1);
    mvc.perform(
            put(path)
                .with(alice())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":6}"))
        .andExpect(status().isBadRequest());
    mvc.perform(delete(path).with(alice()).with(csrf())).andExpect(status().isNoContent());
  }

  @Test
  void databaseRejectsDuplicateAndOutOfRangeRating() {
    sql.update(
        "INSERT INTO personal_rating(user_id,movie_id,score) VALUES(?,?,?)", alice, movie, 4);
    assertThatThrownBy(
            () ->
                sql.update(
                    "INSERT INTO personal_rating(user_id,movie_id,score) VALUES(?,?,?)",
                    alice,
                    movie,
                    3))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                sql.update(
                    "INSERT INTO personal_rating(user_id,movie_id,score) VALUES(?,?,?)",
                    bob,
                    movie,
                    9))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  @Test
  void sharesCanBeReadAnonymouslyAndRevoked() throws Exception {
    mvc.perform(put(collectionPath() + "/movies/" + movie).with(alice()).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(
            post(collectionPath() + "/shares")
                .with(bob())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isNotFound());
    String json =
        mvc.perform(
                post(collectionPath() + "/shares")
                    .with(alice())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String path = com.jayway.jsonpath.JsonPath.read(json, "$.path");
    Number id = com.jayway.jsonpath.JsonPath.read(json, "$.id");
    mvc.perform(get(path))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.movies.content[0].id").value(movie))
        .andExpect(jsonPath("$.userId").doesNotExist())
        .andExpect(jsonPath("$.movies.content[0].personalRating").doesNotExist());
    mvc.perform(get(collectionPath() + "/shares").with(alice()))
        .andExpect(jsonPath("$[0].token").doesNotExist());
    mvc.perform(delete(collectionPath() + "/shares/" + id).with(bob()).with(csrf()))
        .andExpect(status().isNotFound());
    mvc.perform(delete(collectionPath() + "/shares/" + id).with(alice()).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get(path)).andExpect(status().isNotFound());
    assertThat(
            sql.count(
                "SELECT COUNT(*) FROM audit_event WHERE actor_user_id=? AND action='SHARE_REVOKED'",
                alice))
        .isEqualTo(1);
  }

  @Test
  void expiredSharesAreRejected() throws Exception {
    String json =
        mvc.perform(
                post(collectionPath() + "/shares")
                    .with(alice())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String path = com.jayway.jsonpath.JsonPath.read(json, "$.path");
    sql.update(
        "UPDATE collection_share SET expires_at=TIMESTAMP WITH TIME ZONE '2000-01-01"
            + " 00:00:00+00:00' WHERE collection_id=?",
        collection);
    mvc.perform(get(path)).andExpect(status().isNotFound());
  }

  @Test
  void searchFiltersAndRatingSourceAreAvailable() throws Exception {
    mvc.perform(
            get("/api/movies")
                .with(alice())
                .param("castId", Long.toString(actor))
                .param("genreId", Long.toString(genre)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(movie));
    mvc.perform(get("/api/movies").param("q", "Director Two").with(alice()))
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(get("/api/people?q=Example&role=CAST").with(alice()))
        .andExpect(jsonPath("$.content[0].id").value(actor));
    mvc.perform(get("/api/genres").with(alice())).andExpect(status().isOk());
    mvc.perform(
            put("/api/movies/" + movie + "/external-ratings/ExampleSource")
                .with(user("alice@example.test").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":8.4,\"scaleMax\":10,\"voteCount\":100}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/movies/" + movie).with(alice()))
        .andExpect(jsonPath("$.externalRatings[0].source").value("ExampleSource"))
        .andExpect(jsonPath("$.credits").isNotEmpty());
  }

  @Test
  void ordinaryUsersCannotMutateCatalogue() throws Exception {
    mvc.perform(delete("/api/movies/" + movie).with(alice()).with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/genres")
                .with(alice())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Action\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void auditIsPrivateReadOnlyAndFilterable() throws Exception {
    mvc.perform(put(collectionPath() + "/movies/" + movie).with(alice()).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/me/audit-events?action=COLLECTION_ITEM_ADDED").with(alice()))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].correlationId").isNotEmpty());
    mvc.perform(get("/api/me/audit-events?action=COLLECTION_ITEM_ADDED").with(bob()))
        .andExpect(jsonPath("$.totalElements").value(0));
    mvc.perform(post("/api/me/audit-events").with(alice()).with(csrf()))
        .andExpect(status().isMethodNotAllowed());
    mvc.perform(
            get("/api/me/audit-events?from=2030-01-01T00:00:00Z&to=2000-01-01T00:00:00Z")
                .with(alice()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void analyticsAreValidatedAndDeduplicated() throws Exception {
    String event =
        "{\"eventId\":\""
            + UUID.randomUUID()
            + "\",\"eventName\":\"RECOMMENDATION_SELECTED\",\"movieId\":"
            + movie
            + ",\"consentGranted\":true}";
    for (int i = 0; i < 2; i++)
      mvc.perform(
              post("/api/analytics/events")
                  .with(alice())
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(event))
          .andExpect(status().isAccepted());
    assertThat(
            sql.count(
                "SELECT COUNT(*) FROM analytics_event WHERE user_id=? AND"
                    + " event_name='recommendation_selected'",
                alice))
        .isEqualTo(1);
    mvc.perform(
            post("/api/analytics/events")
                .with(bob())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(event))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/analytics/events")
                .with(alice())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(event.replace("true", "false")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void localAiSearchRecommendationsAndFallback() throws Exception {
    mvc.perform(
            post("/api/search/natural-language")
                .with(alice())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"funny space movies\",\"limit\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.engine").value("local-tfidf-v1"))
        .andExpect(jsonPath("$.fallbackUsed").value(false));
    mvc.perform(put(collectionPath() + "/movies/" + movie).with(alice()).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(
            post("/api/recommendations")
                .with(alice())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].movie.id").value(otherMovie));
    mvc.perform(
            post("/api/search/natural-language")
                .with(alice())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"xyzunknown\"}"))
        .andExpect(jsonPath("$.fallbackUsed").value(true));
    assertThat(sql.count("SELECT COUNT(*) FROM ai_request WHERE user_id=?", alice)).isEqualTo(3);
  }

  @Test
  void failedCollectionWriteLeavesNoAuditOrItem() throws Exception {
    mvc.perform(put(collectionPath() + "/movies/999999999").with(alice()).with(csrf()))
        .andExpect(status().isNotFound());
    assertThat(sql.count("SELECT COUNT(*) FROM collection_item WHERE collection_id=?", collection))
        .isZero();
    assertThat(
            sql.count(
                "SELECT COUNT(*) FROM audit_event WHERE actor_user_id=? AND"
                    + " action='COLLECTION_ITEM_ADDED'",
                alice))
        .isZero();
  }

  @Test
  void openApiIncludesControllerCategories() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/me/collections']").exists())
        .andExpect(jsonPath("$.paths['/api/auth/login']").exists())
        .andExpect(jsonPath("$.paths['/api/search/natural-language']").exists())
        .andExpect(jsonPath("$.paths['/api/me/audit-events']").exists());
  }

  @Test
  void analyticsRejectsUnknownFieldsAndInvalidMovie() throws Exception {
    String event =
        "{\"eventId\":\""
            + UUID.randomUUID()
            + "\",\"eventName\":\"RECOMMENDATION_VIEWED\",\"movieId\":"
            + movie
            + ",\"consentGranted\":true,\"rawPrompt\":\"private text\"}";
    mvc.perform(
            post("/api/analytics/events")
                .with(alice())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(event))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/analytics/events")
                .with(alice())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"eventId\":\""
                        + UUID.randomUUID()
                        + "\",\"eventName\":\"RECOMMENDATION_VIEWED\",\"movieId\":999999999,\"consentGranted\":true}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsFractionalRatings() throws Exception {
    mvc.perform(
            put("/api/me/ratings/" + movie)
                .with(alice())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":4.5}"))
        .andExpect(status().isBadRequest());
  }
}
