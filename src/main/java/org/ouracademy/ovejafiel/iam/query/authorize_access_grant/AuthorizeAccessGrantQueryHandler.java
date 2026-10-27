package org.ouracademy.ovejafiel.iam.query.authorize_access_grant;

/**
 * Passes only when all of these hold; otherwise it fails with {@code AccessDeniedException} (403)
 * or {@code DomainValidationException} (400):
 *
 * <ul>
 *   <li>the caller has the required permission ({@code iam.manage} by default) on the node;
 *   <li>the role exists, is not Owner and is a predefined role or one of the node's church;
 *   <li>the caller holds the role or every one of its permissions on the node, so nobody hands out
 *       more access than they have.
 * </ul>
 */
public interface AuthorizeAccessGrantQueryHandler {
  AuthorizeAccessGrantResult handle(AuthorizeAccessGrantQuery query);
}
