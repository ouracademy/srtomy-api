package org.ouracademy.ovejafiel.accountability.query.get_accountabilities_by_parent_and_types;

import java.util.List;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.party.domain.PartyId;

public record GetAccountabilitiesByParentAndTypesQuery(
    PartyId parentId, List<AccountabilityTypeId> typeIds) {}
