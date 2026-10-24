package org.ouracademy.srtomy.iam.query.list_access;

import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.iam.domain.RoleBindingId;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.domain.UserId;

/**
 * One role binding as seen from a node. {@code inherited} is true when it was made on an ancestor
 * ({@code organizationId} says which one).
 */
public record AccessEntryResult(
    RoleBindingId bindingId,
    UserId userId,
    String email,
    IamRoleId roleId,
    String roleName,
    PartyId organizationId,
    String organizationName,
    boolean inherited) {}
