package org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** The parties {@code memberId} IS_PART_OF (for a person: the nodes it belongs to). */
public record FindOrganizationsOfMemberQuery(PartyId memberId) {}
