package org.ouracademy.srtomy.accountability.query.find_organizations_of_member;

import java.util.List;
import org.ouracademy.srtomy.party.domain.PartyId;

public record OrganizationsOfMemberResult(List<PartyId> organizationIds) {}
