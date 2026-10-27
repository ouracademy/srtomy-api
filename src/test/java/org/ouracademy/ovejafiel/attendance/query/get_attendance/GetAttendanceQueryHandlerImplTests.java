package org.ouracademy.ovejafiel.attendance.query.get_attendance;

import static org.assertj.core.api.Assertions.assertThat;
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

    assertThat(result).contains(new GetAttendanceResult(eventId, List.of(firstId, secondId)));
  }

  @Test
  void isEmptyWhenNoAttendanceWasRegistered() {
    var eventId = EventId.generate();
    when(eventAttendanceRepository.findByEventId(eventId)).thenReturn(Optional.empty());

    assertThat(handler.handle(new GetAttendanceQuery(eventId))).isEmpty();
  }
}
