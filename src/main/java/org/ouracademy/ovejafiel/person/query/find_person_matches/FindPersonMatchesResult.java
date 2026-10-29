package org.ouracademy.ovejafiel.person.query.find_person_matches;

import java.time.LocalDateTime;
import java.util.List;
import org.ouracademy.ovejafiel.party.domain.PartyId;

public record FindPersonMatchesResult(List<Match> matches) {

  /**
   * {@code active} is whether they belong to some node of the church today; if not, {@code
   * removedTime} says since when.
   */
  public record Match(
      PartyId id,
      String fullName,
      String email,
      String phoneNumber,
      boolean active,
      LocalDateTime removedTime) {}
}
