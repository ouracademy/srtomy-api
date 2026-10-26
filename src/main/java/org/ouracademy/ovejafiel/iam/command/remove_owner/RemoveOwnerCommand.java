package org.ouracademy.ovejafiel.iam.command.remove_owner;

import org.ouracademy.ovejafiel.iam.domain.RoleBindingId;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Removes an Owner of the church that {@code organizationId} belongs to. Only an Owner can, and the
 * church keeps at least one.
 */
public record RemoveOwnerCommand(String subject, RoleBindingId bindingId, PartyId organizationId) {}
