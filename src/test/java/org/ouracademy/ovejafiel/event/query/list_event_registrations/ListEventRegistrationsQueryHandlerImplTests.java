package org.ouracademy.ovejafiel.event.query.list_event_registrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.event.EventFixtures.ticket;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.AppUser;

class ListEventRegistrationsQueryHandlerImplTests {

  EventRegistrationRepository repository = mock(EventRegistrationRepository.class);
  ListEventRegistrationsQueryHandlerImpl handler =
      new ListEventRegistrationsQueryHandlerImpl(repository);

  @Test
  void listsWhoHoldsOrAwaitsASeatWithTheirTicketAndCheckIn() {
    var id = EventId.generate();
    var event =
        singleEvent(
            id,
            "Curso",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            List.of());
    var general = ticket(event, "General", 5);
    var checkedIn =
        new EventRegistration(
            event, general, null, "Lu", "Mora", "lu@x.org", "999", RegistrationStatus.CONFIRMED);
    checkedIn.checkIn(CheckInSource.QR_TICKET);
    var other =
        new EventRegistration(
            event,
            general,
            new AppUser("me"),
            "Ana",
            "Paz",
            "ana@x.org",
            null,
            RegistrationStatus.CONFIRMED);
    when(repository.findByEventIdAndStatusNotOrderByCreateDate(id, RegistrationStatus.CANCELLED))
        .thenReturn(List.of(checkedIn, other));

    var result = handler.handle(new ListEventRegistrationsQuery(id));

    assertThat(result)
        .extracting(EventRegistrationResult::email)
        .containsExactly("lu@x.org", "ana@x.org");
    assertThat(result.get(0).hasAccount()).isFalse();
    assertThat(result.get(0).phoneNumber()).isEqualTo("999");
    assertThat(result.get(0).ticketName()).isEqualTo("General");
    assertThat(result.get(0).checkedInAt()).isNotNull();
    assertThat(result.get(0).checkInSource()).isEqualTo(CheckInSource.QR_TICKET);
    assertThat(result.get(1).hasAccount()).isTrue();
    assertThat(result.get(1).status()).isEqualTo(RegistrationStatus.CONFIRMED);
    assertThat(result.get(1).checkedInAt()).isNull();
  }
}
