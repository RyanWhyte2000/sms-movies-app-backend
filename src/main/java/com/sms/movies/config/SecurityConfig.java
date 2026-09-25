package com.sms.movies.config;

import com.sms.movies.repository.UserRepository;
import java.util.List;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.*;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
  }

  @Bean
  UserDetailsService userDetailsService(UserRepository users) {
    return email -> {
      var account = users.find(com.sms.movies.service.AccountService.normalizeEmail(email));
      if (account == null) throw new UsernameNotFoundException("Invalid credentials");
      return User.withUsername(account.user().email())
          .password(account.passwordHash())
          .roles(account.role())
          .build();
    };
  }

  @Bean
  AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
    var provider = new DaoAuthenticationProvider(users);
    provider.setPasswordEncoder(encoder);
    return new ProviderManager(provider);
  }

  @Bean
  SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  CsrfTokenRepository csrfTokenRepository() {
    return new HttpSessionCsrfTokenRepository();
  }

  @Bean
  SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrf) {
    return new CompositeSessionAuthenticationStrategy(
        List.of(new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrf)));
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.cors(org.springframework.security.config.Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .csrf(csrf -> csrf.disable())
        .requestCache(c -> c.disable())
        .formLogin(c -> c.disable())
        .httpBasic(c -> c.disable())
        .logout(c -> c.disable())
        .build();
  }

  @Bean
  org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource(
      @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:}")
          String origins) {
    var config = new org.springframework.web.cors.CorsConfiguration();
    var allowed =
        java.util.Arrays.stream(origins.split(","))
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .toList();
    if (allowed.contains("*"))
      throw new IllegalArgumentException("CORS requires explicit origins when using cookies.");
    config.setAllowedOrigins(allowed);
    config.setAllowCredentials(true);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
    config.setExposedHeaders(
        List.of(
            "X-Correlation-ID",
            "Location",
            "RateLimit-Limit",
            "RateLimit-Remaining",
            "RateLimit-Reset",
            "Retry-After"));
    var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }

  private static void problem(
      jakarta.servlet.http.HttpServletResponse res, int status, String detail)
      throws java.io.IOException {
    res.setStatus(status);
    res.setContentType("application/problem+json");
    String title =
        switch (status) {
          case 401 -> "Unauthorized";
          case 403 -> "Forbidden";
          default -> "Request failed";
        };
    res.getWriter()
        .write(
            "{\"type\":\"about:blank\",\"title\":\""
                + title
                + "\",\"status\":"
                + status
                + ",\"detail\":\""
                + detail
                + "\",\"correlationId\":\""
                + com.sms.movies.service.AuditService.correlationId()
                + "\"}");
  }
}
