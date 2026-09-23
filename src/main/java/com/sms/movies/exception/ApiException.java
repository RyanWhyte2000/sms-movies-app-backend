package com.sms.movies.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
  private final HttpStatus status;

  public ApiException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatus status() {
    return status;
  }

  public static ApiException notFound() {
    return new ApiException(HttpStatus.NOT_FOUND, "Resource was not found.");
  }
}
