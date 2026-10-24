package org.ouracademy.srtomy.organization.query.find_organization_subtree;

import java.util.List;
import org.ouracademy.srtomy.party.domain.PartyId;

public record FindOrganizationSubtreeResult(List<PartyId> ids) {}
