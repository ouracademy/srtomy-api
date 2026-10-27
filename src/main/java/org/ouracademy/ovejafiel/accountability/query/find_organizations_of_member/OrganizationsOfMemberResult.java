package org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member;

import java.util.List;
import org.ouracademy.ovejafiel.party.domain.PartyId;

public record OrganizationsOfMemberResult(List<PartyId> organizationIds) {}
