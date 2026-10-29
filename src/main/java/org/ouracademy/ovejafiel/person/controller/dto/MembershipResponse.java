package org.ouracademy.ovejafiel.person.controller.dto;

import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.person.query.get_person_memberships.PersonMembershipResult;

public record MembershipResponse(
    UUID organizationId,
    String organizationName,
    String organizationType,
    MemberState memberState,
    boolean leader,
    List<String> positions) {

  public static MembershipResponse from(PersonMembershipResult result) {
    return new MembershipResponse(
        result.organizationId().value(),
        result.organizationName(),
        result.organizationType() == null ? null : result.organizationType().name(),
        result.state(),
        result.leader(),
        result.positions());
  }
}
