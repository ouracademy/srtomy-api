package org.ouracademy.ovejafiel.event.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.registration;
import static org.ouracademy.ovejafiel.event.EventFixtures.registrationSetup;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.event.EventFixtures.ticket;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.domain.TicketTypeId;
import org.ouracademy.ovejafiel.event.query.Seats;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class WaitlistPromotionTests {

  EventRegistrationRepository registrations = mock(EventRegistrationRepository.class);
  Seats seats = mock(Seats.class);
  WaitlistPromotion promotion = new WaitlistPromotion(registrations, seats);

  Event event =
      singleEvent(
          EventId.generate(),
          "Curso",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @Test
  void promotesWhoWaitedLongestWhileThereIsRoom() {
    var setup = registrationSetup(event, true, 1);
    var general = ticket(event, "General", null);
    var first = registration(event, general, "a@x.org", RegistrationStatus.WAITLISTED);
    var second = registration(event, general, "b@x.org", RegistrationStatus.WAITLISTED);
    when(registrations.findByEventIdAndStatusOrderByCreateDate(
            event.getId(), RegistrationStatus.WAITLISTED))
        .thenReturn(List.of(first, second));
    when(seats.confirmedByTicket(event.getId())).thenReturn(new HashMap<>());
    // One seat in total: the real ledger would say no once the first took it.
    when(seats.hasRoom(setup, general, new HashMap<>())).thenReturn(true);
    when(seats.hasRoom(setup, general, Map.of(general.getId(), 1L))).thenReturn(false);

    var promoted = promotion.promote(setup);

    assertThat(promoted).isEqualTo(1);
    assertThat(first.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
    assertThat(second.getStatus()).isEqualTo(RegistrationStatus.WAITLISTED);
  }

  @Test
  void skipsWhoDoesNotFitAndPromotesTheNextOneThatDoes() {
    var setup = registrationSetup(event, true, null);
    var full = ticket(event, "VIP", 1);
    var open = ticket(event, "General", null);
    var waitingForVip = registration(event, full, "a@x.org", RegistrationStatus.WAITLISTED);
    var waitingForGeneral = registration(event, open, "b@x.org", RegistrationStatus.WAITLISTED);
    when(registrations.findByEventIdAndStatusOrderByCreateDate(
            event.getId(), RegistrationStatus.WAITLISTED))
        .thenReturn(List.of(waitingForVip, waitingForGeneral));
    when(seats.confirmedByTicket(event.getId())).thenReturn(new HashMap<>());
    when(seats.hasRoom(setup, full, new HashMap<TicketTypeId, Long>())).thenReturn(false);
    when(seats.hasRoom(setup, open, new HashMap<TicketTypeId, Long>())).thenReturn(true);

    assertThat(promotion.promote(setup)).isEqualTo(1);

    assertThat(waitingForVip.getStatus()).isEqualTo(RegistrationStatus.WAITLISTED);
    assertThat(waitingForGeneral.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
  }

  @Test
  void nobodyWaitingMeansNothingToPromote() {
    var setup = registrationSetup(event, true, null);
    when(registrations.findByEventIdAndStatusOrderByCreateDate(
            event.getId(), RegistrationStatus.WAITLISTED))
        .thenReturn(List.of());
    when(seats.confirmedByTicket(event.getId())).thenReturn(new HashMap<>());

    assertThat(promotion.promote(setup)).isZero();
  }
}
