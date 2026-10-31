package org.ouracademy.ovejafiel.event.query.find_registrations_to_remind;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.party.domain.PartyId;

public record FindRegistrationsToRemindResult(List<Item> registrations) {

  /**
   * One sign-up to remind: has a seat, left a phone, and has not been checked in.
   *
   * @param start in the time zone of the event, so it reads as the people there will read it
   * @param organizationId the node the event belongs to
   */
  public record Item(
      UUID registrationId,
      String firstName,
      String phone,
      String eventTitle,
      ZonedDateTime start,
      String address,
      PartyId organizationId) {}
}
