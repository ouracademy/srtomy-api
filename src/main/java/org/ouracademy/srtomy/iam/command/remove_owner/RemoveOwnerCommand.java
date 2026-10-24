package org.ouracademy.srtomy.iam.command.remove_owner;

import org.ouracademy.srtomy.iam.domain.RoleBindingId;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * Removes an Owner of the church that {@code organizationId} belongs to. Only an Owner can, and the
 * church keeps at least one.
 */
public record RemoveOwnerCommand(String subject, RoleBindingId bindingId, PartyId organizationId) {}
