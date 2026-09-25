package com.sms.movies.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class Sha256ShareTokenCodec implements ShareTokenCodec {
  private static final int TOKEN_BYTES = 32;
  private static final Pattern TOKEN_FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");

  private final SecureRandom random;

  public Sha256ShareTokenCodec() {
    this(new SecureRandom());
  }

  Sha256ShareTokenCodec(SecureRandom random) {
    this.random = random;
  }

  @Override
  public String generate() {
    byte[] bytes = new byte[TOKEN_BYTES];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  @Override
  public String digest(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  @Override
  public boolean isValid(String token) {
    return token != null && TOKEN_FORMAT.matcher(token).matches();
  }
}
