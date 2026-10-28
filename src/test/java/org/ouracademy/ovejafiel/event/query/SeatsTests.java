package org.ouracademy.ovejafiel.event.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.registrationSetup;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.event.EventFixtures.ticket;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class SeatsTests {

  EventRegistrationRepository registrations = mock(EventRegistrationRepository.class);
  Seats seats = new Seats(registrations);

  EventId id = EventId.generate();
  Event event =
      singleEvent(
          id,
          "Curso",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @Test
  void countsOnlyTheConfirmedSignUpsOfEachTicket() {
    var general = ticket(event, "General", 10);
    when(registrations.countByTicketType(id, RegistrationStatus.CONFIRMED))
        .thenReturn(List.<Object[]>of(new Object[] {general.getId(), 4L}));

    assertThat(seats.confirmedByTicket(id)).containsEntry(general.getId(), 4L);
  }

  @Test
  void aTicketWithoutLimitsNeverRunsOut() {
    var setup = registrationSetup(event);
    var free = ticket(event, "Libre", null);

    assertThat(seats.remaining(setup, free, Map.of(free.getId(), 500L))).isNull();
    assertThat(seats.hasRoom(setup, free, Map.of(free.getId(), 500L))).isTrue();
  }

  @Test
  void aTicketRunsOutAtItsQuantity() {
    var setup = registrationSetup(event);
    var vip = ticket(event, "VIP", 2);

    assertThat(seats.remaining(setup, vip, Map.of(vip.getId(), 1L))).isEqualTo(1);
    assertThat(seats.hasRoom(setup, vip, Map.of(vip.getId(), 2L))).isFalse();
  }
}
