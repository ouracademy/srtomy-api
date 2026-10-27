package org.ouracademy.ovejafiel.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class TokenClaimsTests {

  static Jwt.Builder jwt() {
    return Jwt.withTokenValue("t")
        .header("alg", "none")
        .subject("u")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60));
  }

  @Test
  void theNamespacedClaimWins() {
    var token =
        jwt()
            .claim("https://sr-tommy.com/email", "ns@x.org")
            .claim("https://sr-tommy.com/email_verified", true)
            .claim("email", "std@x.org")
            .build();

    assertThat(TokenClaims.email(token)).isEqualTo("ns@x.org");
    assertThat(TokenClaims.emailVerified(token)).isTrue();
  }

  @Test
  void theStandardClaimIsTheFallbackAndMissingMeansUnverified() {
    var token = jwt().claim("email", "std@x.org").build();

    assertThat(TokenClaims.email(token)).isEqualTo("std@x.org");
    assertThat(TokenClaims.emailVerified(token)).isFalse();
    assertThat(TokenClaims.email(jwt().build())).isNull();
  }
}
