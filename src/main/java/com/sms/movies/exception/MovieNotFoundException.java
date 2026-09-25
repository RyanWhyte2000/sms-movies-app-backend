package com.sms.movies.exception;

public class MovieNotFoundException extends RuntimeException {
  public MovieNotFoundException(java.util.UUID id) {
    super("Movie with ID " + id + " was not found.");
  }
}
