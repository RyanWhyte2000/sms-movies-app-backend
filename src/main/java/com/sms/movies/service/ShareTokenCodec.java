package com.sms.movies.service;

/** Generates and verifies the opaque token material used by collection share links. */
public interface ShareTokenCodec {
  String generate();

  String digest(String token);

  boolean isValid(String token);
}
