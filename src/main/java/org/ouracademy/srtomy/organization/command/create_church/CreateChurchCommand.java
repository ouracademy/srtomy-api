package org.ouracademy.srtomy.organization.command.create_church;

import org.ouracademy.srtomy.organization.command.OrganizationInfo;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * Creates a church and stores its id in the Auth0 app_metadata of {@code userId}.
 *
 * @param parentId optional parent organization
 */
public record CreateChurchCommand(OrganizationInfo info, PartyId parentId, String userId) {}
