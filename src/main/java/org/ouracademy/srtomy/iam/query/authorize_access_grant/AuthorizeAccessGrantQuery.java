package org.ouracademy.srtomy.iam.query.authorize_access_grant;

import java.util.Set;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * Asks whether the user identified by {@code subject} may make {@code roleIds} and the loose {@code
 * permissions} what a position gives (or hand that out through one) on {@code organizationId}.
 */
public record AuthorizeAccessGrantQuery(
    String subject, Set<IamRoleId> roleIds, Set<String> permissions, PartyId organizationId) {}
