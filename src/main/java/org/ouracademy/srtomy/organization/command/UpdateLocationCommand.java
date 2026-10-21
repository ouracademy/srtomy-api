package org.ouracademy.srtomy.organization.command;

import java.util.SortedSet;
import org.ouracademy.srtomy.party.domain.PartyId;

/** Overwrites a location's info and syncs its custom roles to exactly {@code roleNames}. */
public record UpdateLocationCommand(
    PartyId id, OrganizationInfo info, SortedSet<String> roleNames) {}
