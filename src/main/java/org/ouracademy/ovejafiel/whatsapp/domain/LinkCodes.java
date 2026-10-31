package org.ouracademy.ovejafiel.whatsapp.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * The one-time code a person sends from their WhatsApp to prove the number is theirs: 8 characters
 * of Crockford's base32 (no I, L, O or U), shown as {@code ABCD-EFGH}. It cannot be guessed in a
 * few tries (40 bits, and only a few wrong tries per number are allowed) and is easy to type.
 */
public final class LinkCodes {

  private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
  private static final int LENGTH = 8;
  private static final SecureRandom RANDOM = new SecureRandom();

  private LinkCodes() {}

  public static String generate() {
    var chars = new StringBuilder(LENGTH);
    for (var i = 0; i < LENGTH; i++) {
      chars.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
    }
    return chars.substring(0, 4) + "-" + chars.substring(4);
  }

  /**
   * What the person typed, without the dash or spaces, in upper case and with the letters people
   * confuse read as Crockford does (O as 0, I and L as 1).
   */
  public static String normalize(String typed) {
    if (typed == null) {
      return "";
    }
    return typed
        .toUpperCase(java.util.Locale.ROOT)
        .replaceAll("[^0-9A-Z]", "")
        .replace('O', '0')
        .replace('I', '1')
        .replace('L', '1');
  }

  /** Only this is stored, never the code. */
  public static String hash(String typed) {
    try {
      var digest =
          MessageDigest.getInstance("SHA-256")
              .digest(normalize(typed).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /** A code has the right shape: 8 characters of the alphabet once normalized. */
  public static boolean looksLikeACode(String typed) {
    var normalized = normalize(typed);
    return normalized.length() == LENGTH
        && normalized.chars().allMatch(c -> ALPHABET.indexOf(c) >= 0);
  }

  /** The digits of a phone, the way numbers are compared and stored. */
  public static String digitsOf(String phone) {
    return phone == null ? "" : phone.replaceAll("[^0-9]", "");
  }
}
