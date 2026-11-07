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
  org.ouracademy.ovejafiel.event.repository.EventRepository eventRepository =
      mock(org.ouracademy.ovejafiel.event.repository.EventRepository.class);
  org.ouracademy.ovejafiel.event.repository.SubEventCheckInRepository subEventCheckIns =
      mock(org.ouracademy.ovejafiel.event.repository.SubEventCheckInRepository.class);
  org.ouracademy.ovejafiel.event.query.InheritedCheckIns inheritedCheckIns =
      new org.ouracademy.ovejafiel.event.query.InheritedCheckIns(subEventCheckIns);
  ListEventRegistrationsQueryHandlerImpl handler =
      new ListEventRegistrationsQueryHandlerImpl(repository, eventRepository, inheritedCheckIns);

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

  @Test
  void aSubEventThatInheritsListsTheSignUpsOfItsMegaEventWithItsOwnEntries() {
    var mega =
        singleEvent(
            EventId.generate(),
            "Congreso",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            List.of());
    var child =
        org.ouracademy.ovejafiel.event.EventFixtures.childOf(
            mega, EventId.generate(), "Taller", mega.getStart(), mega.getEnd());
    when(eventRepository.findById(child.getId())).thenReturn(java.util.Optional.of(child));
    var general = ticket(mega, "General", 5);
    var came =
        new EventRegistration(
            mega, general, null, "Ana", "Paz", "a@x.org", null, RegistrationStatus.CONFIRMED);
    var expected =
        new EventRegistration(
            mega, general, null, "Lu", "Mora", "l@x.org", null, RegistrationStatus.CONFIRMED);
    // Lu came to the mega event but not into this workshop: that does not count here.
    expected.checkIn(CheckInSource.MANUAL);
    when(repository.findByEventIdAndStatusNotOrderByCreateDate(
            mega.getId(), RegistrationStatus.CANCELLED))
        .thenReturn(List.of(came, expected));
    var entry =
        new org.ouracademy.ovejafiel.event.domain.SubEventCheckIn(
            came, child, CheckInSource.QR_TICKET);
    when(subEventCheckIns.findByEventId(child.getId())).thenReturn(List.of(entry));

    var result = handler.handle(new ListEventRegistrationsQuery(child.getId()));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).checkedInAt()).isEqualTo(entry.getCheckedInAt());
    assertThat(result.get(0).checkInSource()).isEqualTo(CheckInSource.QR_TICKET);
    assertThat(result.get(1).checkedInAt()).isNull();
  }
}
