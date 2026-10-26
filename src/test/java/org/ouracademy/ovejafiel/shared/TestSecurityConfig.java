package org.ouracademy.ovejafiel.shared;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Replaces SecurityConfig's real JwtDecoder (which calls out to Auth0's OIDC discovery endpoint at
 * startup — never acceptable in tests) with one backed by a key pair generated in memory. Also
 * provides a JwtEncoder so tests can mint their own valid/invalid tokens, with no network call and
 * no dependency on any real Auth0 tenant.
 */
@TestConfiguration
public class TestSecurityConfig {

  public static final String VALID_AUDIENCE = SecurityConfig.AUDIENCE;

  @Bean
  public RSAKey testRsaKey() throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    var keyPair = generator.generateKeyPair();
    return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
        .privateKey((RSAPrivateKey) keyPair.getPrivate())
        .keyID(UUID.randomUUID().toString())
        .build();
  }

  @Bean
  public JwtDecoder jwtDecoder(RSAKey rsaKey) throws Exception {
    var decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
    // No real issuer to validate against here (tokens use a fake test issuer), but the
    // audience check is exactly what SecurityConfigTest exercises, so it must be wired
    // in the same way the production decoder wires it.
    decoder.setJwtValidator(new AudienceValidator(VALID_AUDIENCE));
    return decoder;
  }

  @Bean
  public JwtEncoder jwtEncoder(RSAKey rsaKey) {
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
  }

  /** A token with the audience this API requires — requests using it should be authenticated. */
  public static String validToken(JwtEncoder encoder) {
    return validToken(encoder, "test-user");
  }

  /** A valid token for the user identified by {@code subject}, e.g. to act as several users. */
  public static String validToken(JwtEncoder encoder, String subject) {
    return token(encoder, subject, List.of(VALID_AUDIENCE));
  }

  /** A token for a *different* API — requests using it should still be rejected. */
  public static String tokenWithWrongAudience(JwtEncoder encoder) {
    return token(encoder, "test-user", List.of("https://some-other-api.example.com"));
  }

  private static String token(JwtEncoder encoder, String subject, List<String> audience) {
    var claims =
        JwtClaimsSet.builder()
            .issuer("https://test-issuer.example.com/")
            .subject(subject)
            .audience(audience)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
    var header = JwsHeader.with(SignatureAlgorithm.RS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
