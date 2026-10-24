package org.ouracademy.srtomy.iam.command.grant_role;

import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.domain.UserId;

/**
 * Binds {@code roleId} to {@code userId} on {@code organizationId}. {@code grantedBy} is null when
 * the system grants it (e.g. Owner to whoever creates a church).
 */
public record GrantRoleCommand(
    UserId userId, IamRoleId roleId, PartyId organizationId, UserId grantedBy) {}
