package org.ouracademy.ovejafiel.person.query.find_people_by_phone;

import java.util.List;
import org.ouracademy.ovejafiel.party.domain.PartyId;

public record FindPeopleByPhoneResult(List<Match> people) {

  /**
   * @param whatsappConsent false when the person asked to stop the notices
   */
  public record Match(PartyId id, String name, boolean whatsappConsent) {

    public Match(PartyId id, String name) {
      this(id, name, true);
    }
  }
}
