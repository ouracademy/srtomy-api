package org.ouracademy.srtomy.iam.command.revoke_access;

import org.ouracademy.srtomy.iam.domain.RoleBindingId;
import org.ouracademy.srtomy.party.domain.PartyId;

/** Removes the binding made directly on {@code organizationId}; inherited ones are not there. */
public record RevokeAccessCommand(
    String subject, RoleBindingId bindingId, PartyId organizationId) {}
