package org.ouracademy.srtomy.accountability.query;

import java.util.List;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.party.domain.PartyId;

public record GetAccountabilitiesByParentAndTypesQuery(
    PartyId parentId, List<AccountabilityTypeId> typeIds) {}
