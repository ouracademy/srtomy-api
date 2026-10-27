package org.ouracademy.ovejafiel.iam.command.grant_role;

import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;

/**
 * Binds {@code roleId} to {@code userId} on {@code organizationId}. {@code grantedBy} is null when
 * the system grants it (e.g. Owner to whoever creates a church).
 */
public record GrantRoleCommand(
    UserId userId, IamRoleId roleId, PartyId organizationId, UserId grantedBy) {}
