package org.ouracademy.ovejafiel.shared;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.JWTParser;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Tokens the API signs itself so the WhatsApp agent can call it as a member (RFC 8693 style: {@code
 * sub} is the member, {@code act.sub} the agent). They are short-lived, carry the same audience as
 * the Auth0 ones and are accepted by the same endpoints, so the roles of the member apply as they
 * are. The signing key is {@code delegation.private-key}; without it no token can be made and none
 * is accepted.
 */
public final class DelegatedTokens {

  public static final String ISSUER = "https://api.srtommy.com/delegated";
  public static final String AGENT = "srtomy-agent";
  public static final Duration LIFETIME = Duration.ofMinutes(10);

  private final RSAKey key;
  private final JwtEncoder encoder;
  private final Clock clock;

  private DelegatedTokens(RSAKey key, Clock clock) {
    this.key = key;
    this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    this.clock = clock;
  }

  public static DelegatedTokens of(RSAKey key) {
    return new DelegatedTokens(key, Clock.systemUTC());
  }

  public static DelegatedTokens of(RSAKey key, Clock clock) {
    return new DelegatedTokens(key, clock);
  }

  /** From a PKCS#8 private key in PEM (or just its base64 body); the public key is derived. */
  public static DelegatedTokens fromPem(String pem) {
    try {
      var body = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
      var factory = KeyFactory.getInstance("RSA");
      var privateKey =
          (RSAPrivateCrtKey)
              factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(body)));
      var publicKey =
          (RSAPublicKey)
              factory.generatePublic(
                  new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
      return of(new RSAKey.Builder(publicKey).privateKey(privateKey).keyIDFromThumbprint().build());
    } catch (Exception e) {
      throw new IllegalStateException("delegation.private-key is not a valid PKCS#8 RSA key", e);
    }
  }

  public record Issued(String token, Instant expiresAt) {}

  public Issued issue(String subject) {
    // A JWT keeps whole seconds; the expiry we report must be the one inside the token.
    var now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    var expiresAt = now.plus(LIFETIME);
    var claims =
        JwtClaimsSet.builder()
            .issuer(ISSUER)
            .audience(java.util.List.of(SecurityConfig.AUDIENCE))
            .subject(subject)
            .issuedAt(now)
            .expiresAt(expiresAt)
            .claim("act", Map.of("sub", AGENT))
            .build();
    var header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build();
    return new Issued(
        encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), expiresAt);
  }

  /** Accepts only our tokens: right issuer, audience, not expired, and made for the agent. */
  public JwtDecoder decoder() {
    try {
      var decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
      OAuth2TokenValidator<Jwt> forTheAgent =
          jwt -> {
            var actor = jwt.getClaimAsMap("act");
            return actor != null && AGENT.equals(actor.get("sub"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Not a token made for the agent", null));
          };
      decoder.setJwtValidator(
          new DelegatingOAuth2TokenValidator<>(
              JwtValidators.createDefaultWithIssuer(ISSUER),
              new AudienceValidator(SecurityConfig.AUDIENCE),
              forTheAgent));
      return decoder;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * A decoder that sends each token to the one that issued it: ours when {@code iss} is {@link
   * #ISSUER}, the {@code other} (Auth0) in any other case.
   */
  public static JwtDecoder routing(JwtDecoder other, DelegatedTokens delegated) {
    var ours = delegated == null ? null : delegated.decoder();
    return token -> {
      if (ours != null && ISSUER.equals(issuerOf(token))) {
        return ours.decode(token);
      }
      return other.decode(token);
    };
  }

  private static String issuerOf(String token) {
    try {
      return JWTParser.parse(token).getJWTClaimsSet().getIssuer();
    } catch (ParseException e) {
      throw new JwtException("Unreadable token", e);
    }
  }
}
