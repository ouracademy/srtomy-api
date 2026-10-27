package org.ouracademy.ovejafiel.shared.domain;

import java.security.SecureRandom;
import java.util.Base64;

/** Addresses of public pages and shared links: random, so nobody can guess one. */
public final class RandomSlugs {

  private static final SecureRandom RANDOM = new SecureRandom();

  private RandomSlugs() {}

  /** 16 URL-safe characters (96 bits of randomness). */
  public static String next() {
    var bytes = new byte[12];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
