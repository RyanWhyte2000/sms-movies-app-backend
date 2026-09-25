package com.sms.movies.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class ApiRateLimitFilterTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-25T12:00:00Z"), ZoneOffset.UTC);

  @Test
  public void rejectsRequestsAfterClientExhaustsWindow() throws Exception {
    var filter = new ApiRateLimitFilter(true, 2, Duration.ofSeconds(60), 100, mapper(), CLOCK);
    var chainCalls = new AtomicInteger();

    MockHttpServletResponse first = invoke(filter, "192.0.2.1", "/api/movies", chainCalls);
    MockHttpServletResponse second = invoke(filter, "192.0.2.1", "/api/movies", chainCalls);
    MockHttpServletResponse third = invoke(filter, "192.0.2.1", "/api/movies", chainCalls);

    Assertions.assertEquals(200, first.getStatus());
    Assertions.assertEquals("1", first.getHeader("RateLimit-Remaining"));
    Assertions.assertEquals("0", second.getHeader("RateLimit-Remaining"));
    Assertions.assertEquals(429, third.getStatus());
    Assertions.assertEquals("60", third.getHeader("Retry-After"));
    Assertions.assertEquals("application/problem+json", third.getContentType());
    Assertions.assertEquals(2, chainCalls.get());
    Assertions.assertEquals(
        429, mapper().readTree(third.getContentAsString()).get("status").asInt());
  }

  @Test
  public void tracksClientsIndependentlyAndIgnoresForwardedAddress() throws Exception {
    var filter = new ApiRateLimitFilter(true, 1, Duration.ofMinutes(1), 100, mapper(), CLOCK);
    var chainCalls = new AtomicInteger();

    MockHttpServletRequest first = request("192.0.2.1", "/api/movies");
    first.addHeader("X-Forwarded-For", "203.0.113.10");
    filter.doFilter(
        first,
        new MockHttpServletResponse(),
        (request, response) -> chainCalls.incrementAndGet());
    MockHttpServletResponse other = invoke(filter, "192.0.2.2", "/api/movies", chainCalls);

    Assertions.assertEquals(200, other.getStatus());
    Assertions.assertEquals(2, chainCalls.get());
  }

  @Test
  public void skipsNonApiAndPreflightRequests() throws Exception {
    var filter = new ApiRateLimitFilter(true, 1, Duration.ofMinutes(1), 100, mapper(), CLOCK);
    var chainCalls = new AtomicInteger();

    invoke(filter, "192.0.2.1", "/swagger-ui/index.html", chainCalls);
    MockHttpServletRequest options = request("192.0.2.1", "/api/movies");
    options.setMethod("OPTIONS");
    filter.doFilter(options, new MockHttpServletResponse(), (request, response) -> chainCalls.incrementAndGet());
    MockHttpServletResponse api = invoke(filter, "192.0.2.1", "/api/movies", chainCalls);

    Assertions.assertEquals(200, api.getStatus());
    Assertions.assertEquals(3, chainCalls.get());
  }

  private static MockHttpServletResponse invoke(
      ApiRateLimitFilter filter, String address, String path, AtomicInteger chainCalls)
      throws Exception {
    var response = new MockHttpServletResponse();
    filter.doFilter(
        request(address, path), response, (request, ignored) -> chainCalls.incrementAndGet());
    return response;
  }

  private static MockHttpServletRequest request(String address, String path) {
    var request = new MockHttpServletRequest("GET", path);
    request.setRemoteAddr(address);
    return request;
  }

  private static ObjectMapper mapper() {
    return new ObjectMapper();
  }
}
