package org.ouracademy.srtomy.accountability.command;

import java.util.SortedSet;
import org.ouracademy.srtomy.party.domain.PartyId;

/** Makes the location's own ROLE accountability types match exactly {@code roleNames}. */
public record SyncOrganizationRolesCommand(PartyId locationId, SortedSet<String> roleNames) {}
