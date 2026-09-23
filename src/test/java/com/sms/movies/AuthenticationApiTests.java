package com.sms.movies;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sms.movies.repository.SqlRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AuthenticationApiTests {
  @Autowired MockMvc mvc;
  @Autowired SqlRepository sql;

  @Test
  void sessionLoginRotatesCsrfAndLogoutInvalidatesSession() throws Exception {
    var csrfResponse =
        mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn().getResponse();
    String token = com.jayway.jsonpath.JsonPath.read(csrfResponse.getContentAsString(), "$.token");
    var cookie = csrfResponse.getCookie("SESSION");
    assertThat(cookie).isNotNull();
    mvc.perform(
            post("/api/auth/register")
                .cookie(cookie)
                .header("X-CSRF-TOKEN", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"session@example.test\",\"password\":\"long-session-password\",\"displayName\":\"Session"
                        + " User\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.password").doesNotExist());
    var login =
        mvc.perform(
                post("/api/auth/login")
                    .cookie(cookie)
                    .header("X-CSRF-TOKEN", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"SESSION@example.test\",\"password\":\"long-session-password\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var authenticated = login.getCookie("SESSION");
    assertThat(authenticated).isNotNull();
    assertThat(authenticated.getValue()).isNotEqualTo(cookie.getValue());
    mvc.perform(get("/api/me").cookie(authenticated))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("session@example.test"));
    mvc.perform(post("/api/auth/logout").cookie(authenticated).header("X-CSRF-TOKEN", token))
        .andExpect(status().isForbidden());
    var refreshed = mvc.perform(get("/api/csrf").cookie(authenticated)).andReturn().getResponse();
    String newToken = com.jayway.jsonpath.JsonPath.read(refreshed.getContentAsString(), "$.token");
    mvc.perform(post("/api/auth/logout").cookie(authenticated).header("X-CSRF-TOKEN", newToken))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/me").cookie(authenticated)).andExpect(status().isUnauthorized());
    assertThat(
            sql.count(
                "SELECT COUNT(*) FROM audit_event WHERE action='LOGIN' AND outcome='SUCCESS'"))
        .isEqualTo(1);
    assertThat(sql.count("SELECT COUNT(*) FROM audit_event WHERE action='LOGOUT'")).isEqualTo(1);
  }

  @Test
  void rejectsDuplicatesWeakPasswordsAndBadLogin() throws Exception {
    String register =
        "{\"email\":\"account@example.test\",\"password\":\"long-account-password\",\"displayName\":\"Account\"}";
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(register))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(register.replace("account@example", "ACCOUNT@example")))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(register.replace("long-account-password", "short")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"account@example.test\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());
    assertThat(
            sql.count(
                "SELECT COUNT(*) FROM audit_event WHERE action='LOGIN' AND outcome='FAILURE'"))
        .isEqualTo(1);
    mvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(register))
        .andExpect(status().isForbidden());
  }
}
