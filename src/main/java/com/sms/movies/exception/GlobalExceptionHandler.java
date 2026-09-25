package com.sms.movies.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ProblemDetail api(ApiException exception) {
    return problem(exception.status(), exception.getMessage());
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ProblemDetail conflict(org.springframework.dao.DataIntegrityViolationException exception) {
    return problem(
        HttpStatus.CONFLICT,
        "The request conflicts with an existing record or a related resource.");
  }

  @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
  public ProblemDetail unauthorized(
      org.springframework.security.core.AuthenticationException exception) {
    return problem(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ProblemDetail denied(org.springframework.security.access.AccessDeniedException exception) {
    return problem(HttpStatus.FORBIDDEN, "Access denied.");
  }

  @ExceptionHandler(MovieNotFoundException.class)
  public ProblemDetail notFound(MovieNotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ProblemDetail notFound(NoSuchElementException exception) {
    return problem(HttpStatus.NOT_FOUND, "Resource was not found.");
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail unexpected(Exception exception) {
    String correlationId = com.sms.movies.service.AuditService.correlationId();
    log.error("Unhandled request failure; correlationId={}", correlationId, exception);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "The server could not complete the request. Please try again later.",
        correlationId);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail problem = problem(status, "Request validation failed.");
    Map<String, String> errors = new LinkedHashMap<>();
    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
    problem.setProperty("errors", errors);
    return handleExceptionInternal(exception, problem, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception exception,
      Object body,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    Object responseBody = body;
    if (responseBody == null) {
      responseBody = problem(status, defaultDetail(status));
    } else if (responseBody instanceof ProblemDetail problem) {
      addCorrelationId(problem);
    }
    return super.handleExceptionInternal(exception, responseBody, headers, status, request);
  }

  private static ProblemDetail problem(HttpStatusCode status, String detail) {
    return problem(status, detail, com.sms.movies.service.AuditService.correlationId());
  }

  private static ProblemDetail problem(HttpStatusCode status, String detail, String correlationId) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setProperty("correlationId", correlationId);
    return problem;
  }

  private static void addCorrelationId(ProblemDetail problem) {
    if (problem.getProperties() == null
        || !problem.getProperties().containsKey("correlationId")) {
      problem.setProperty("correlationId", com.sms.movies.service.AuditService.correlationId());
    }
  }

  private static String defaultDetail(HttpStatusCode status) {
    return switch (status.value()) {
      case 400 -> "The request is invalid.";
      case 401 -> "Sign in to continue.";
      case 403 -> "Access denied.";
      case 404 -> "Resource was not found.";
      case 409 -> "The request conflicts with the current resource state.";
      default -> "The request could not be completed.";
    };
  }
}
