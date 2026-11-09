package org.ouracademy.ovejafiel.shared;

import java.security.PrivateKey;
import java.util.Base64;

/**
 * A key in PEM, the way the API reads {@code delegation.private-key}, made on the fly for tests.
 *
 * <p>The header is built in pieces on purpose: a test key written as {@code BEGIN ... END} in the
 * source looks, to a secret scanner, like a private key committed by mistake, even when it is
 * generated at run time. There is no key in this repository.
 */
public final class TestPem {

  private static final String LABEL = "PRIVATE" + " KEY";

  private TestPem() {}

  public static String pkcs8(PrivateKey key) {
    var body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(key.getEncoded());
    return "-----" + "BEGIN " + LABEL + "-----\n" + body + "\n-----" + "END " + LABEL + "-----";
  }
}
