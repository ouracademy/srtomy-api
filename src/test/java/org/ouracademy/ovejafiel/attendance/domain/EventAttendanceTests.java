package org.ouracademy.ovejafiel.attendance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ouracademy.ovejafiel.attendance.AttendanceFixtures.eventAttendance;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;
import static org.ouracademy.ovejafiel.person.PersonFixtures.person;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.domain.AppUser;

class EventAttendanceTests {

  Event event =
      singleEvent(
          EventId.generate(),
          "Culto",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @Test
  void markingSomeoneRecordsHowAndByWhom() {
    var attendance = eventAttendance(event, List.of());
    var usher = new AppUser("usher");

    var added =
        attendance.markPresent(
            person(PartyId.generate(), "Ana", "Paz"), CheckInSource.QR_TICKET, usher);

    assertThat(added).isTrue();
    var mark = attendance.getPersonEventAttendances().get(0);
    assertThat(mark.getSource()).isEqualTo(CheckInSource.QR_TICKET);
    assertThat(mark.getMarkedBy()).isSameAs(usher);
    assertThat(mark.getDateTime()).isNotNull();
  }

  @Test
  void markingSomeoneTwiceChangesNothing() {
    var attendance = eventAttendance(event, List.of());
    var ana = person(PartyId.generate(), "Ana", "Paz");
    attendance.markPresent(ana, CheckInSource.QR_EVENT, null);

    var again = attendance.markPresent(ana, CheckInSource.MANUAL, null);

    assertThat(again).isFalse();
    assertThat(attendance.getPersonEventAttendances()).hasSize(1);
    assertThat(attendance.getPersonEventAttendances().get(0).getSource())
        .isEqualTo(CheckInSource.QR_EVENT);
  }

  @Test
  void replacingTheListKeepsHowThoseWhoStayWereMarked() {
    var attendance = eventAttendance(event, List.of());
    var ana = person(PartyId.generate(), "Ana", "Paz");
    var luis = person(PartyId.generate(), "Luis", "Rey");
    var eva = person(PartyId.generate(), "Eva", "Sol");
    attendance.markPresent(ana, CheckInSource.QR_EVENT, null);
    attendance.markPresent(luis, CheckInSource.QR_TICKET, null);

    attendance.setPeople(List.of(ana, eva));

    assertThat(attendance.getPersonIds()).containsExactlyInAnyOrder(ana.getId(), eva.getId());
    var byPerson =
        attendance.getPersonEventAttendances().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    mark -> mark.getPerson().getId(), PersonEventAttendance::getSource));
    assertThat(byPerson.get(ana.getId())).isEqualTo(CheckInSource.QR_EVENT);
    assertThat(byPerson.get(eva.getId())).isEqualTo(CheckInSource.MANUAL);
  }

  @Test
  void theGuestsWithoutANameCannotBeNegative() {
    var attendance = eventAttendance(event, List.of());

    attendance.setGuestCount(5);
    assertThat(attendance.getGuestCount()).isEqualTo(5);
    assertThatThrownBy(() -> attendance.setGuestCount(-1))
        .isInstanceOf(DomainValidationException.class);
    assertThat(attendance.getGuestCount()).isEqualTo(5);
  }
}
