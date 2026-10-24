package org.ouracademy.srtomy.organization.domain;

import java.security.SecureRandom;
import java.util.Base64;

/** Addresses of public pages: random, so nobody can guess the page of a node. */
final class Slugs {

  private static final SecureRandom RANDOM = new SecureRandom();

  private Slugs() {}

  /** 16 URL-safe characters (96 bits of randomness). */
  static String random() {
    var bytes = new byte[12];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
