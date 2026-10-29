package org.ouracademy.ovejafiel.person.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import org.ouracademy.ovejafiel.person.query.find_person_matches.FindPersonMatchesResult.Match;

/**
 * A person who may be the one being registered. {@code status} is ACTIVE while they belong to some
 * node of the church and REMOVED after they were taken out ({@code removedTime} says when).
 */
public record PersonMatchResponse(
    UUID id,
    String fullName,
    String email,
    String phoneNumber,
    Status status,
    LocalDateTime removedTime) {

  public enum Status {
    ACTIVE,
    REMOVED
  }

  public static PersonMatchResponse from(Match match) {
    return new PersonMatchResponse(
        match.id().value(),
        match.fullName(),
        match.email(),
        match.phoneNumber(),
        match.active() ? Status.ACTIVE : Status.REMOVED,
        match.active() ? null : match.removedTime());
  }
}
