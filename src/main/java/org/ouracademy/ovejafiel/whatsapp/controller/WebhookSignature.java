package org.ouracademy.ovejafiel.whatsapp.controller;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Kapso signs the raw body of a webhook with HMAC-SHA256 and the secret of the endpoint, and sends
 * the hex in {@code X-Webhook-Signature}. It has to be checked against the bytes received, never
 * against a re-serialized object.
 */
final class WebhookSignature {

  private WebhookSignature() {}

  /** With no secret configured nothing is valid. */
  static boolean isValid(byte[] rawBody, String signature, String secret) {
    if (secret == null || secret.isBlank() || signature == null) {
      return false;
    }
    var expected = HexFormat.of().formatHex(hmac(rawBody, secret));
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
  }

  static byte[] hmac(byte[] body, String secret) {
    try {
      var mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal(body);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException(e);
    }
  }
}
