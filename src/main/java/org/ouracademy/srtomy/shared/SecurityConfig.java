package org.ouracademy.srtomy.shared;

import org.ouracademy.srtomy.auth.gateway.Auth0Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every endpoint requires a valid Auth0 JWT, except the actuator health probes (public, so
 * Kubernetes liveness/readiness checks work without credentials). Flat "authenticated = full
 * access" for now — no per-role authorization yet.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  static final String AUDIENCE = "https://api.srtommy.com";
  private static final String[] SWAGGER_WHITELIST = {
    "/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html"
  };

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, Environment environment) {
    boolean local = environment.matchesProfiles("local");
    http.cors(Customizer.withDefaults())
        .authorizeHttpRequests(
            authorize -> {
              authorize.requestMatchers("/actuator/health/**").permitAll();
              // Swagger UI is only ever exposed locally — Cloud Run (dev and prod)
              // always runs with the "prod" profile, so this never opens it up there.
              if (local) {
                authorize.requestMatchers(SWAGGER_WHITELIST).permitAll();
              }
              authorize.anyRequest().authenticated();
            })
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Safe here: this is a stateless JWT resource server (no cookies, no
        // session, SessionCreationPolicy.STATELESS above). CSRF protection
        // exists to stop a browser from automatically attaching a victim's
        // *ambient* credentials (session cookies) to a forged cross-site
        // request; a Bearer token in an Authorization header is never
        // attached by the browser automatically, so there's no ambient
        // credential for a forged request to ride on. This is Spring
        // Security's own documented guidance for stateless token-based APIs.
        .csrf(csrf -> csrf.disable())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
    return http.build();
  }

  /**
   * {@code @ConditionalOnMissingBean} so tests can supply their own {@link JwtDecoder} (a local key
   * pair, no network) instead of this one, which calls out to Auth0's real OIDC discovery endpoint
   * at startup via {@link JwtDecoders#fromIssuerLocation} — tests must never depend on that being
   * reachable.
   */
  @Bean
  @ConditionalOnMissingBean(JwtDecoder.class)
  public JwtDecoder jwtDecoder(Auth0Config auth0Config) {
    var issuerUri = auth0Config.getUrl() + "/";
    var decoder =
        (org.springframework.security.oauth2.jwt.NimbusJwtDecoder)
            JwtDecoders.fromIssuerLocation(issuerUri);

    OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
    OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(AUDIENCE);
    OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience, withTimestamp));
    return decoder;
  }
}
