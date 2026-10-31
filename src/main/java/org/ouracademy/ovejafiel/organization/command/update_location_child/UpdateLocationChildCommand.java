package org.ouracademy.ovejafiel.organization.command.update_location_child;

import org.ouracademy.ovejafiel.organization.command.OrganizationInfo;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Edits a group or ministry. Its people are not touched: they are managed one by one with their
 * memberships (see {@code SetNodeMembershipCommand}).
 */
public record UpdateLocationChildCommand(
    PartyId id, OrganizationInfo info, Organization.Type type) {}
