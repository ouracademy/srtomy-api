package org.ouracademy.srtomy.shared;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Reads the user's contact claims from an access token. Auth0 only adds custom claims to access
 * tokens under a namespace, so the namespaced claim set by the project's Auth0 Action is read first
 * and the standard OIDC claim is the fallback.
 */
public final class TokenClaims {

  static final String NAMESPACE = "https://sr-tommy.com/";

  private TokenClaims() {}

  public static String email(Jwt jwt) {
    var namespaced = jwt.getClaimAsString(NAMESPACE + "email");
    return namespaced != null ? namespaced : jwt.getClaimAsString("email");
  }

  public static boolean emailVerified(Jwt jwt) {
    var namespaced = jwt.getClaimAsBoolean(NAMESPACE + "email_verified");
    var verified = namespaced != null ? namespaced : jwt.getClaimAsBoolean("email_verified");
    return Boolean.TRUE.equals(verified);
  }
}
