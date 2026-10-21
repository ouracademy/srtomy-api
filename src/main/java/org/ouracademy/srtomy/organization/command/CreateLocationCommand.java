package org.ouracademy.srtomy.organization.command;

import java.util.SortedSet;
import org.ouracademy.srtomy.party.domain.PartyId;

/** Creates a location under {@code parentId} with its own custom role names. */
public record CreateLocationCommand(
    OrganizationInfo info, PartyId parentId, SortedSet<String> roleNames) {}
