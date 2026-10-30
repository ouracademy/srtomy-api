package org.ouracademy.ovejafiel.organization.query.find_organization_ancestors;

import java.util.List;
import org.ouracademy.ovejafiel.party.domain.PartyId;

public record FindOrganizationAncestorsResult(List<PartyId> ids) {}
