package org.ouracademy.srtomy.iam.query.authorize;

/**
 * Fails with Spring Security's {@code AccessDeniedException} (rendered as 403) when the permission
 * is not granted, so callers guard an endpoint with a single call.
 */
public interface AuthorizeQueryHandler {
  AuthorizeResult handle(AuthorizeQuery query);
}
