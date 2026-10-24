package org.ouracademy.srtomy.iam.query.authorize_access_grant;

import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * Asks whether the user identified by {@code subject} may make {@code roleId} the access level of a
 * position (or hand it out through one) on {@code organizationId}.
 */
public record AuthorizeAccessGrantQuery(String subject, IamRoleId roleId, PartyId organizationId) {}
