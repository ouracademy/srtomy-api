package org.ouracademy.ovejafiel.whatsapp.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class WebhookSignatureTests {

  private static final byte[] BODY = "{\"phone_number_id\":\"1\"}".getBytes(StandardCharsets.UTF_8);

  private static String sign(byte[] body, String secret) {
    return HexFormat.of().formatHex(WebhookSignature.hmac(body, secret));
  }

  @Test
  void acceptsTheSignatureOfTheExactBody() {
    assertThat(WebhookSignature.isValid(BODY, sign(BODY, "s3cret"), "s3cret")).isTrue();
  }

  @Test
  void matchesTheKnownHmacSha256Vector() {
    // RFC 4231, test case 2.
    var body = "what do ya want for nothing?".getBytes(StandardCharsets.UTF_8);

    assertThat(sign(body, "Jefe"))
        .isEqualTo("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843");
  }

  @Test
  void refusesAnotherBodyAnotherSecretOrNoSignature() {
    var signature = sign(BODY, "s3cret");

    assertThat(WebhookSignature.isValid("{}".getBytes(StandardCharsets.UTF_8), signature, "s3cret"))
        .isFalse();
    assertThat(WebhookSignature.isValid(BODY, signature, "other")).isFalse();
    assertThat(WebhookSignature.isValid(BODY, null, "s3cret")).isFalse();
    assertThat(WebhookSignature.isValid(BODY, "", "s3cret")).isFalse();
  }

  @Test
  void refusesEverythingWhenNoSecretIsConfigured() {
    assertThat(WebhookSignature.isValid(BODY, sign(BODY, "x"), "")).isFalse();
    assertThat(WebhookSignature.isValid(BODY, sign(BODY, "x"), null)).isFalse();
  }
}
