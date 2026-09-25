package com.sms.movies.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Sha256ShareTokenCodecTest {
  private final ShareTokenCodec codec = new Sha256ShareTokenCodec();

  @Test
  void generatesUrlSafeTokensWithStableDigests() {
    String first = codec.generate();
    String second = codec.generate();

    assertTrue(codec.isValid(first));
    assertTrue(codec.isValid(second));
    assertNotEquals(first, second);
    assertEquals(codec.digest(first), codec.digest(first));
    assertEquals(64, codec.digest(first).length());
  }

  @Test
  void rejectsMalformedTokens() {
    assertFalse(codec.isValid(null));
    assertFalse(codec.isValid("too-short"));
    assertFalse(codec.isValid("!".repeat(43)));
  }
}
