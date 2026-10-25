package org.ouracademy.srtomy.iam.query.authorize_access_grant;

import java.util.Set;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * Asks whether the user identified by {@code subject} may make {@code roleIds} and the loose {@code
 * permissions} what a position gives (or hand that out through one) on {@code organizationId}. The
 * caller needs {@code required} on the node ({@code iam.manage} unless said otherwise; an
 * invitation only needs {@code invitations.send}).
 */
public record AuthorizeAccessGrantQuery(
    String subject,
    Set<IamRoleId> roleIds,
    Set<String> permissions,
    PartyId organizationId,
    Permission required) {

  /** Giving a position access needs {@code iam.manage}. */
  public AuthorizeAccessGrantQuery(
      String subject, Set<IamRoleId> roleIds, Set<String> permissions, PartyId organizationId) {
    this(subject, roleIds, permissions, organizationId, Permission.IAM_MANAGE);
  }
}
