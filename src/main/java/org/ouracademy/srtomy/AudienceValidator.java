package org.ouracademy.srtomy;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Spring Security validates {@code iss}/{@code exp}/{@code nbf} out of the box, but not {@code aud}
 * — Auth0 only puts our API's audience in a token when a client actually requested one for it, so
 * this is what rejects an otherwise-valid Auth0 token that was never meant for this API.
 */
public record AudienceValidator(String requiredAudience) implements OAuth2TokenValidator<Jwt> {

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    if (jwt.getAudience().contains(requiredAudience)) {
      return OAuth2TokenValidatorResult.success();
    }
    return OAuth2TokenValidatorResult.failure(
        new OAuth2Error("invalid_token", "The required audience is missing", null));
  }
}
