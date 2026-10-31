package org.ouracademy.ovejafiel.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class DelegatedTokensTests {

  static RSAKey key;
  static RSAKey otherKey;

  @BeforeAll
  static void keys() throws Exception {
    key = newKey();
    otherKey = newKey();
  }

  static RSAKey newKey() throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    var pair = generator.generateKeyPair();
    return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
        .privateKey((RSAPrivateKey) pair.getPrivate())
        .keyIDFromThumbprint()
        .build();
  }

  @Test
  void aTokenCarriesTheMemberTheAgentAndAShortLife() {
    var tokens = DelegatedTokens.of(key);

    var issued = tokens.issue("whatsapp:abc");
    Jwt jwt = tokens.decoder().decode(issued.token());

    assertThat(jwt.getSubject()).isEqualTo("whatsapp:abc");
    assertThat(jwt.getIssuer().toString()).isEqualTo(DelegatedTokens.ISSUER);
    assertThat(jwt.getAudience()).containsExactly(SecurityConfig.AUDIENCE);
    assertThat(jwt.<java.util.Map<String, Object>>getClaim("act"))
        .containsEntry("sub", DelegatedTokens.AGENT);
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
        .isEqualTo(DelegatedTokens.LIFETIME);
    assertThat(issued.expiresAt()).isEqualTo(jwt.getExpiresAt());
  }

  @Test
  void anExpiredTokenIsRefused() {
    var past = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
    var issued = DelegatedTokens.of(key, past).issue("whatsapp:abc");

    assertThatThrownBy(() -> DelegatedTokens.of(key).decoder().decode(issued.token()))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void aTokenSignedByAnotherKeyIsRefused() {
    var forged = DelegatedTokens.of(otherKey).issue("auth0|owner");

    assertThatThrownBy(() -> DelegatedTokens.of(key).decoder().decode(forged.token()))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void aTokenWithoutTheAgentAsActorIsRefused() {
    var claims =
        JwtClaimsSet.builder()
            .issuer(DelegatedTokens.ISSUER)
            .audience(List.of(SecurityConfig.AUDIENCE))
            .subject("whatsapp:abc")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
    var token =
        new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)))
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build(), claims))
            .getTokenValue();

    assertThatThrownBy(() -> DelegatedTokens.of(key).decoder().decode(token))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void aTokenForAnotherApiIsRefused() {
    var claims =
        JwtClaimsSet.builder()
            .issuer(DelegatedTokens.ISSUER)
            .audience(List.of("https://some-other-api.example.com"))
            .subject("whatsapp:abc")
            .claim("act", java.util.Map.of("sub", DelegatedTokens.AGENT))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
    var token =
        new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)))
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build(), claims))
            .getTokenValue();

    assertThatThrownBy(() -> DelegatedTokens.of(key).decoder().decode(token))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void theKeyCanBeReadFromAPem() throws Exception {
    var pem =
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(key.toRSAPrivateKey().getEncoded())
            + "\n-----END PRIVATE KEY-----";

    var tokens = DelegatedTokens.fromPem(pem);

    var jwt = tokens.decoder().decode(tokens.issue("whatsapp:x").token());
    assertThat(jwt.getSubject()).isEqualTo("whatsapp:x");
    // The same key as the original one: its tokens are accepted by a decoder made from it.
    assertThat(DelegatedTokens.of(key).decoder().decode(tokens.issue("whatsapp:y").token()))
        .isNotNull();
  }

  @Test
  void garbageIsNotAKey() {
    assertThatThrownBy(() -> DelegatedTokens.fromPem("not a key"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void routingSendsEachTokenToItsIssuer() {
    var tokens = DelegatedTokens.of(key);
    var calls = new java.util.ArrayList<String>();
    JwtDecoder auth0 =
        token -> {
          calls.add(token);
          return Jwt.withTokenValue(token).header("alg", "none").subject("auth0|x").build();
        };
    var routed = DelegatedTokens.routing(auth0, tokens);

    var ours = tokens.issue("whatsapp:abc").token();

    assertThat(routed.decode(ours).getSubject()).isEqualTo("whatsapp:abc");
    assertThat(calls).isEmpty();
    var theirs = TestTokens.signedBy(otherKey, "https://srtommy.us.auth0.com/");
    assertThat(routed.decode(theirs).getSubject()).isEqualTo("auth0|x");
    assertThat(calls).containsExactly(theirs);
  }

  @Test
  void withoutAKeyOurIssuerIsNotAccepted() {
    var forged = DelegatedTokens.of(key).issue("auth0|owner").token();
    var seen = new java.util.ArrayList<String>();
    JwtDecoder auth0 =
        token -> {
          seen.add(token);
          throw new JwtException("not an Auth0 token");
        };

    assertThatThrownBy(() -> DelegatedTokens.routing(auth0, null).decode(forged))
        .isInstanceOf(JwtException.class);
    assertThat(seen).containsExactly(forged);
  }

  @Test
  void anUnreadableTokenIsRefused() {
    var routed = DelegatedTokens.routing(token -> null, DelegatedTokens.of(key));

    assertThatThrownBy(() -> routed.decode("garbage")).isInstanceOf(JwtException.class);
  }

  /** Just enough to mint a token with a given issuer. */
  static final class TestTokens {
    static String signedBy(RSAKey key, String issuer) {
      var claims =
          JwtClaimsSet.builder()
              .issuer(issuer)
              .subject("auth0|x")
              .issuedAt(Instant.now())
              .expiresAt(Instant.now().plusSeconds(60))
              .build();
      return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)))
          .encode(
              JwtEncoderParameters.from(
                  JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build(), claims))
          .getTokenValue();
    }
  }
}
