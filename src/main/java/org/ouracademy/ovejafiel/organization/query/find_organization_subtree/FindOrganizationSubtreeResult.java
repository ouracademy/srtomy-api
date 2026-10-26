package org.ouracademy.ovejafiel.organization.query.find_organization_subtree;

import java.util.List;
import org.ouracademy.ovejafiel.party.domain.PartyId;

public record FindOrganizationSubtreeResult(List<PartyId> ids) {}
