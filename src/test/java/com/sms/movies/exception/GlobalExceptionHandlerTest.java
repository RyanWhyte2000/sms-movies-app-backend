package com.sms.movies.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.WebRequest;

class GlobalExceptionHandlerTest {
  @Test
  void mapsKnownAndUnexpectedFailuresToProblems() {
    var handler = new GlobalExceptionHandler();
    assertEquals(400, handler.api(new ApiException(HttpStatus.BAD_REQUEST, "bad")).getStatus());
    assertEquals(409, handler.conflict(new DataIntegrityViolationException("duplicate")).getStatus());
    assertEquals(401, handler.unauthorized(new BadCredentialsException("bad")).getStatus());
    assertEquals(403, handler.denied(new AccessDeniedException("no")).getStatus());
    assertEquals(404, handler.notFound(new MovieNotFoundException(UUID.randomUUID())).getStatus());
    assertEquals(404, handler.notFound(new NoSuchElementException()).getStatus());
    var unexpected = handler.unexpected(new RuntimeException("secret"));
    assertEquals(500, unexpected.getStatus());
    assertFalse(unexpected.getDetail().contains("secret"));
    assertNotNull(unexpected.getProperties().get("correlationId"));
  }

  @Test
  void internalHandlerAddsDefaultBodyAndCorrelation() {
    var handler = new ExposedHandler();
    var request = mock(WebRequest.class);
    var response = handler.internal(new RuntimeException(), null, HttpStatus.NOT_FOUND, request);
    var problem = (ProblemDetail) response.getBody();
    assertEquals("Resource was not found.", problem.getDetail());
    assertNotNull(problem.getProperties().get("correlationId"));

    var supplied = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    var second = handler.internal(new RuntimeException(), supplied, HttpStatus.CONFLICT, request);
    assertNotNull(((ProblemDetail) second.getBody()).getProperties().get("correlationId"));
  }

  private static final class ExposedHandler extends GlobalExceptionHandler {
    ResponseEntity<Object> internal(Exception exception, Object body, HttpStatus status, WebRequest request) {
      return handleExceptionInternal(exception, body, new HttpHeaders(), status, request);
    }
  }
}
