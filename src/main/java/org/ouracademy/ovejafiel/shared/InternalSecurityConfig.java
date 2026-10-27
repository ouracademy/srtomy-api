package org.ouracademy.ovejafiel.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * {@code /v1/internal/**} is for Cloud Scheduler, not for the web app: it is reached with a Google
 * ID token (OIDC) of the scheduler's service account, never with an Auth0 token. The token must
 * name that account ({@code internal.scheduler-email}) and the audience {@code internal.audience};
 * with no account configured nothing gets in.
 */
@Configuration
public class InternalSecurityConfig {

  static final String GOOGLE_ISSUER = "https://accounts.google.com";
  private static final String GOOGLE_JWKS = "https://www.googleapis.com/oauth2/v3/certs";

  /** Wraps the decoder so it does not take the place of the Auth0 {@link JwtDecoder} bean. */
  public record InternalJwtDecoder(JwtDecoder decoder) {}

  @Bean
  @Order(1)
  SecurityFilterChain internalFilterChain(HttpSecurity http, InternalJwtDecoder internal)
      throws Exception {
    http.securityMatcher("/v1/internal/**")
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Same reasoning as SecurityConfig: a Bearer token is not an ambient credential.
        .csrf(csrf -> csrf.disable())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(internal.decoder())));
    return http.build();
  }

  /** Tests supply their own, with a local key pair and no network. */
  @Bean
  @ConditionalOnMissingBean(InternalJwtDecoder.class)
  InternalJwtDecoder internalJwtDecoder(
      @Value("${internal.audience:https://api.srtommy.com/internal}") String audience,
      @Value("${internal.scheduler-email:}") String schedulerEmail) {
    // Fetches Google's keys on the first token, not at startup.
    var decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS).build();
    decoder.setJwtValidator(validators(audience, schedulerEmail));
    return new InternalJwtDecoder(decoder);
  }

  static OAuth2TokenValidator<Jwt> validators(String audience, String schedulerEmail) {
    OAuth2TokenValidator<Jwt> fromScheduler =
        token -> {
          var email = token.getClaimAsString("email");
          var verified = Boolean.TRUE.equals(token.getClaimAsBoolean("email_verified"));
          if (!schedulerEmail.isBlank() && verified && schedulerEmail.equals(email)) {
            return OAuth2TokenValidatorResult.success();
          }
          return OAuth2TokenValidatorResult.failure(
              new OAuth2Error("invalid_token", "Not the scheduler's service account", null));
        };
    return new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(GOOGLE_ISSUER),
        new AudienceValidator(audience),
        fromScheduler);
  }
}
