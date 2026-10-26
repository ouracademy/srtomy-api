package org.ouracademy.ovejafiel.organization.query.get_member_state;

import org.ouracademy.ovejafiel.accountability.domain.AccountabilityId;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.organization.query.PersonDetails;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * One party's member progress state (the accountability type) in an organization. {@code
 * childPerson} is only set when the child is a person.
 */
public record MemberStateResult(
    AccountabilityId id,
    PartyId childId,
    String childName,
    PartyId parentId,
    AccountabilityTypeId stateId,
    String stateName,
    PersonDetails childPerson) {}
