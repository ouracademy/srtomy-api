package org.ouracademy.ovejafiel.attendance.query.get_attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.attendance.AttendanceFixtures.eventAttendance;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;
import static org.ouracademy.ovejafiel.person.PersonFixtures.person;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.attendance.repository.EventAttendanceRepository;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class GetAttendanceQueryHandlerImplTests {

  @Mock EventAttendanceRepository eventAttendanceRepository;

  GetAttendanceQueryHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler = new GetAttendanceQueryHandlerImpl(eventAttendanceRepository);
  }

  @Test
  void returnsTheEventIdAndAttendeeIds() {
    var eventId = EventId.generate();
    var firstId = PartyId.generate();
    var secondId = PartyId.generate();
    var event =
        singleEvent(
            eventId,
            "Culto",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            List.of());
    var attendance =
        eventAttendance(
            event, List.of(person(firstId, "Jose", "Mena"), person(secondId, "Ana", "Rojas")));
    when(eventAttendanceRepository.findByEventId(eventId)).thenReturn(Optional.of(attendance));

    var result = handler.handle(new GetAttendanceQuery(eventId));

    assertThat(result).isPresent();
    assertThat(result.get().eventId()).isEqualTo(eventId);
    assertThat(result.get().personIds()).containsExactly(firstId, secondId);
    assertThat(result.get().guestCount()).isZero();
    assertThat(result.get().marks())
        .extracting(GetAttendanceResult.Mark::personId, GetAttendanceResult.Mark::source)
        .containsExactly(
            tuple(firstId, CheckInSource.MANUAL), tuple(secondId, CheckInSource.MANUAL));
  }

  @Test
  void includesTheVisitorsWithoutNameAndHowEachPersonWasMarked() {
    var eventId = EventId.generate();
    var personId = PartyId.generate();
    var event =
        singleEvent(
            eventId,
            "Culto",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            List.of());
    var attendance = eventAttendance(event, List.of());
    attendance.markPresent(person(personId, "Ana", "Rojas"), CheckInSource.QR_EVENT, null);
    attendance.setGuestCount(5);
    when(eventAttendanceRepository.findByEventId(eventId)).thenReturn(Optional.of(attendance));

    var result = handler.handle(new GetAttendanceQuery(eventId)).orElseThrow();

    assertThat(result.guestCount()).isEqualTo(5);
    assertThat(result.marks().get(0).source()).isEqualTo(CheckInSource.QR_EVENT);
    assertThat(result.marks().get(0).markedAt()).isNotNull();
  }

  @Test
  void isEmptyWhenNoAttendanceWasRegistered() {
    var eventId = EventId.generate();
    when(eventAttendanceRepository.findByEventId(eventId)).thenReturn(Optional.empty());

    assertThat(handler.handle(new GetAttendanceQuery(eventId))).isEmpty();
  }
}
