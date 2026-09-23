package com.sms.movies.exception;

public class MovieNotFoundException extends RuntimeException {
  public MovieNotFoundException(long id) {
    super("Movie with ID " + id + " was not found.");
  }
}
