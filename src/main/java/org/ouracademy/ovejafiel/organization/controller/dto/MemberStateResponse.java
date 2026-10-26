package org.ouracademy.ovejafiel.organization.controller.dto;

import java.time.LocalDate;
import java.util.UUID;
import org.ouracademy.ovejafiel.organization.query.PersonDetails;
import org.ouracademy.ovejafiel.organization.query.get_member_state.MemberStateResult;

/**
 * Keeps the nesting of the Accountability JSON this endpoint used to return ({@code child.name},
 * {@code child.firstName}, {@code type.name}, ...). The person fields of {@code child} are null
 * when the child is an organization; {@code parent} only carries its id.
 */
public record MemberStateResponse(UUID id, PartyRef child, PartyRef parent, TypeRef type) {

  public record PartyRef(
      UUID id,
      String name,
      String fullName,
      String firstName,
      String lastName,
      String phoneNumber,
      Integer age,
      LocalDate birthdate,
      Boolean hasBeenBaptized) {

    static PartyRef of(UUID id, String name, PersonDetails person) {
      if (person == null) {
        return new PartyRef(id, name, null, null, null, null, null, null, null);
      }
      return new PartyRef(
          id,
          name,
          name,
          person.firstName(),
          person.lastName(),
          person.phoneNumber(),
          person.age(),
          person.birthdate(),
          person.hasBeenBaptized());
    }
  }

  public record TypeRef(UUID id, String name) {}

  public static MemberStateResponse from(MemberStateResult result) {
    return new MemberStateResponse(
        result.id().value(),
        PartyRef.of(result.childId().value(), result.childName(), result.childPerson()),
        PartyRef.of(result.parentId().value(), null, null),
        new TypeRef(result.stateId().value(), result.stateName()));
  }
}
