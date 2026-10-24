package org.ouracademy.srtomy.invitation.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Invitation tokens: random, sent by email, and stored only as a SHA-256 hash. */
public final class InvitationTokens {

  private static final SecureRandom RANDOM = new SecureRandom();

  private InvitationTokens() {}

  public static String newToken() {
    var bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static String hash(String token) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
