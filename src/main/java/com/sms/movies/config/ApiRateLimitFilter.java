package com.sms.movies.config;

import com.sms.movies.service.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-instance API rate limiter keyed by the direct client address.
 *
 * <p>The direct address is intentional: forwarded headers must only be interpreted by trusted
 * infrastructure. Deployments with more than one application replica should enforce a shared
 * limit at the ingress or replace this store with a distributed implementation.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiRateLimitFilter extends OncePerRequestFilter {
  private static final long CLEANUP_INTERVAL = 1024;

  private final boolean enabled;
  private final int requestLimit;
  private final long windowSeconds;
  private final int maxTrackedClients;
  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();
  private final AtomicLong requestCounter = new AtomicLong();

  @Autowired
  public ApiRateLimitFilter(
      @Value("${app.rate-limit.enabled:true}") boolean enabled,
      @Value("${app.rate-limit.requests:120}") int requestLimit,
      @Value("${app.rate-limit.window:60s}") java.time.Duration window,
      @Value("${app.rate-limit.max-tracked-clients:100000}") int maxTrackedClients) {
    this(enabled, requestLimit, window, maxTrackedClients, Clock.systemUTC());
  }

  ApiRateLimitFilter(
      boolean enabled,
      int requestLimit,
      java.time.Duration window,
      int maxTrackedClients,
      Clock clock) {
    if (requestLimit < 1) throw new IllegalArgumentException("Rate limit must be positive");
    if (window.isZero() || window.isNegative())
      throw new IllegalArgumentException("Rate limit window must be positive");
    if (maxTrackedClients < 1)
      throw new IllegalArgumentException("Maximum tracked clients must be positive");
    this.enabled = enabled;
    this.requestLimit = requestLimit;
    this.windowSeconds = Math.max(1, window.toSeconds());
    this.maxTrackedClients = maxTrackedClients;
    this.clock = clock;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !enabled
        || "OPTIONS".equalsIgnoreCase(request.getMethod())
        || !(path.equals("/api") || path.startsWith("/api/"));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long now = clock.instant().getEpochSecond();
    if (requestCounter.incrementAndGet() % CLEANUP_INTERVAL == 0) {
      windows.entrySet().removeIf(entry -> entry.getValue().resetAt() <= now);
    }

    String client = request.getRemoteAddr();
    if (!windows.containsKey(client) && windows.size() >= maxTrackedClients) {
      windows.entrySet().removeIf(entry -> entry.getValue().resetAt() <= now);
      if (windows.size() >= maxTrackedClients) {
        reject(response, 1);
        return;
      }
    }

    Decision[] result = new Decision[1];
    windows.compute(
        client,
        (key, current) -> {
          Window window = current;
          boolean allowed;
          if (window == null || window.resetAt() <= now) {
            window = new Window(1, now + windowSeconds);
            allowed = true;
          } else if (window.requests() < requestLimit) {
            window = new Window(window.requests() + 1, window.resetAt());
            allowed = true;
          } else {
            allowed = false;
          }
          result[0] =
              new Decision(
                  allowed,
                  Math.max(0, requestLimit - window.requests()),
                  window.resetAt());
          return window;
        });

    Decision decision = result[0];
    addHeaders(response, decision.remaining(), decision.resetAt(), now);
    if (!decision.allowed()) {
      reject(response, secondsUntil(decision.resetAt(), now));
      return;
    }
    chain.doFilter(request, response);
  }

  private void reject(HttpServletResponse response, long retryAfter) throws IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setHeader("Retry-After", Long.toString(Math.max(1, retryAfter)));
    response
        .getWriter()
        .write(
            "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\","
                + "\"status\":429,\"detail\":\"Too many requests. Please try again later.\","
                + "\"correlationId\":\""
                + AuditService.correlationId()
                + "\"}");
  }

  private void addHeaders(HttpServletResponse response, int remaining, long resetAt, long now) {
    long resetSeconds = secondsUntil(resetAt, now);
    response.setHeader("RateLimit-Limit", Integer.toString(requestLimit));
    response.setHeader("RateLimit-Remaining", Integer.toString(remaining));
    response.setHeader("RateLimit-Reset", Long.toString(resetSeconds));
    response.setHeader("X-RateLimit-Limit", Integer.toString(requestLimit));
    response.setHeader("X-RateLimit-Remaining", Integer.toString(remaining));
    response.setHeader("X-RateLimit-Reset", Long.toString(resetAt));
  }

  private static long secondsUntil(long resetAt, long now) {
    return Math.max(1, resetAt - now);
  }

  private record Window(int requests, long resetAt) {}

  private record Decision(boolean allowed, int remaining, long resetAt) {}
}
