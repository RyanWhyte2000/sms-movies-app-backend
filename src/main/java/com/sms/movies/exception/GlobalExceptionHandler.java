package com.sms.movies.exception;

import java.util.LinkedHashMap;
import java.util.Map;
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
  @ExceptionHandler(ApiException.class)
  public ProblemDetail api(ApiException exception) {
    var problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
    problem.setProperty("correlationId", com.sms.movies.service.AuditService.correlationId());
    return problem;
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ProblemDetail conflict(org.springframework.dao.DataIntegrityViolationException exception) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        "The request conflicts with an existing record or a related resource.");
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ProblemDetail denied(org.springframework.security.access.AccessDeniedException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied.");
  }

  @ExceptionHandler(MovieNotFoundException.class)
  public ProblemDetail notFound(MovieNotFoundException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Movie validation failed.");
    Map<String, String> errors = new LinkedHashMap<>();
    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
    problem.setProperty("errors", errors);
    return handleExceptionInternal(exception, problem, headers, status, request);
  }
}
