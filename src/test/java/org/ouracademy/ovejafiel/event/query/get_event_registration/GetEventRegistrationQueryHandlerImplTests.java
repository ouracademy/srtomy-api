package org.ouracademy.ovejafiel.event.query.get_event_registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.registrationSetup;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.event.EventFixtures.ticket;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.Seats;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.TicketTypeRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class GetEventRegistrationQueryHandlerImplTests {

  EventRepository events = mock(EventRepository.class);
  EventRegistrationSetupRepository setups = mock(EventRegistrationSetupRepository.class);
  TicketTypeRepository tickets = mock(TicketTypeRepository.class);
  EventRegistrationRepository registrations = mock(EventRegistrationRepository.class);
  Seats seats = mock(Seats.class);
  GetEventRegistrationQueryHandlerImpl handler =
      new GetEventRegistrationQueryHandlerImpl(events, setups, tickets, registrations, seats);

  EventId id = EventId.generate();
  Event event =
      singleEvent(
          id,
          "Curso",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @Test
  void describesTheSetupWithWhatEachTicketHasSold() {
    var setup = registrationSetup(event, true, 40);
    var general = ticket(event, "General", 30);
    when(events.findById(id)).thenReturn(Optional.of(event));
    when(setups.findByEventId(id)).thenReturn(Optional.of(setup));
    when(tickets.findByEventIdOrderByPosition(id)).thenReturn(List.of(general));
    when(seats.confirmedByTicket(id)).thenReturn(Map.of(general.getId(), 7L));
    when(registrations.countByEventIdAndStatus(id, RegistrationStatus.CONFIRMED)).thenReturn(7L);
    when(registrations.countByEventIdAndStatus(id, RegistrationStatus.WAITLISTED)).thenReturn(2L);
    when(registrations.countCheckedIn(id)).thenReturn(5L);

    var result = handler.handle(new GetEventRegistrationQuery(id)).orElseThrow();

    assertThat(result.enabled()).isTrue();
    assertThat(result.open()).isTrue();
    assertThat(result.waitlistEnabled()).isTrue();
    assertThat(result.capacity()).isEqualTo(40);
    assertThat(result.confirmed()).isEqualTo(7);
    assertThat(result.waitlisted()).isEqualTo(2);
    assertThat(result.checkedIn()).isEqualTo(5);
    assertThat(result.tickets()).hasSize(1);
    assertThat(result.tickets().get(0).sold()).isEqualTo(7);
    assertThat(result.tickets().get(0).quantity()).isEqualTo(30);
  }

  @Test
  void anEventWithoutRegistrationIsDisabled() {
    when(events.findById(id)).thenReturn(Optional.of(event));
    when(setups.findByEventId(id)).thenReturn(Optional.empty());

    var result = handler.handle(new GetEventRegistrationQuery(id)).orElseThrow();

    assertThat(result.enabled()).isFalse();
    assertThat(result.tickets()).isEmpty();
  }

  @Test
  void anUnknownEventIsEmpty() {
    when(events.findById(id)).thenReturn(Optional.empty());

    assertThat(handler.handle(new GetEventRegistrationQuery(id))).isEmpty();
  }
}
