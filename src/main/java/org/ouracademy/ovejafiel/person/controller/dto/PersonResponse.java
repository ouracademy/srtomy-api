package org.ouracademy.ovejafiel.person.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.person.query.FindPersonResult;
import org.ouracademy.ovejafiel.person.query.search_people.SearchPeopleResult.Standing;

/** Same JSON fields the endpoints returned when they serialized the Person entity. */
public record PersonResponse(
    UUID id,
    String name,
    String fullName,
    String firstName,
    String lastName,
    String description,
    String phoneNumber,
    String email,
    String address,
    Integer age,
    LocalDate birthdate,
    Boolean hasBeenBaptized,
    LocalDateTime createDate,
    LocalDateTime lastModifiedDate,
    MemberState memberState,
    Boolean leader) {

  /** The person alone, without how they stand in any node. */
  public static PersonResponse from(FindPersonResult result) {
    return from(result, null);
  }

  /**
   * The person and, when given, how they stand in the node that was listed: {@code memberState} is
   * their state there and {@code leader} whether they lead it; both null for someone who is only in
   * a node below it.
   */
  public static PersonResponse from(FindPersonResult result, Standing standing) {
    return new PersonResponse(
        result.id().value(),
        result.name(),
        result.name(),
        result.firstName(),
        result.lastName(),
        result.description(),
        result.phoneNumber(),
        result.email(),
        result.address(),
        result.age(),
        result.birthdate(),
        result.hasBeenBaptized(),
        result.createDate(),
        result.lastModifiedDate(),
        standing == null ? null : standing.state(),
        standing == null ? null : standing.leader());
  }
}
