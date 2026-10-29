package org.ouracademy.ovejafiel.person.query.get_person_memberships;

import java.util.List;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.MemberState;

/**
 * A person in one node: their {@code state} there (null if they have none), whether they {@code
 * leader} it and the names of the positions (cargos) they hold in it.
 */
public record PersonMembershipResult(
    PartyId organizationId,
    String organizationName,
    Organization.Type organizationType,
    MemberState state,
    boolean leader,
    List<String> positions) {}
