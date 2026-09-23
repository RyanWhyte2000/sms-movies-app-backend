package com.sms.movies;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sms.movies.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@org.springframework.transaction.annotation.Transactional
@WithMockUser(username = "admin@example.test", roles = "ADMIN")
class MovieApiTests {
  private static final String MOVIE =
      """
      {"title":"Inception","description":"A dream within a dream.",
       "director":"Christopher Nolan","genre":"Science Fiction",
       "releaseDate":"2010-07-16","durationMinutes":148}
      """;

  @Autowired MockMvc mvc;
  @Autowired MovieRepository repository;
  @Autowired com.sms.movies.service.AccountService accounts;

  @BeforeEach
  void clearMovies() {
    repository.deleteAll();
    accounts.register(
        new com.sms.movies.dto.AccountDtos.RegisterRequest(
            "admin@example.test", "a-long-test-password", "Admin"));
  }

  @Test
  void movieLifecycle() throws Exception {
    String location =
        mvc.perform(
                post("/api/movies")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(MOVIE))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").isNumber())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    mvc.perform(get(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Inception"));
    mvc.perform(get("/api/movies").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].title").value("Inception"))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.size").value(1));
    mvc.perform(
            put(location)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(MOVIE.replace("Inception", "Updated title")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated title"));
    mvc.perform(get(location)).andExpect(jsonPath("$.title").value("Updated title"));
    mvc.perform(delete(location).with(csrf())).andExpect(status().isNoContent());
    mvc.perform(get(location))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void rejectsInvalidMovie() throws Exception {
    mvc.perform(
            post("/api/movies")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(MOVIE.replace("Inception", " ").replace("148", "0")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.title").exists())
        .andExpect(jsonPath("$.errors.durationMinutes").exists());
  }

  @Test
  void rejectsInvalidPaginationAndIdentifiers() throws Exception {
    mvc.perform(get("/api/movies?page=-1")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/movies?size=101")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/movies?size=0")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/movies/0")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/movies/not-a-number")).andExpect(status().isBadRequest());
  }

  @Test
  void missingMoviesCannotBeUpdatedOrDeleted() throws Exception {
    mvc.perform(
            put("/api/movies/999999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(MOVIE))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/movies/999999").with(csrf())).andExpect(status().isNotFound());
  }

  @Test
  void malformedJsonReturnsBadRequest() throws Exception {
    mvc.perform(
            post("/api/movies").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void requiresAuthenticationAndCsrf() throws Exception {
    mvc.perform(get("/api/movies").with(anonymous()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/movies").contentType(MediaType.APPLICATION_JSON).content(MOVIE))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/csrf"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
        .andExpect(jsonPath("$.token").isNotEmpty());
  }
}
