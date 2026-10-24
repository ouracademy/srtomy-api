package org.ouracademy.srtomy.iam.command.grant_access;

import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.party.domain.PartyId;

/** The user with {@code subject} grants {@code roleId} on a node to the user with this email. */
public record GrantAccessCommand(
    String subject, String email, IamRoleId roleId, PartyId organizationId) {}
