package org.ouracademy.srtomy.attendance.command.delete_attendance_for_events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.event.EventFixtures.eventSeries;
import static org.ouracademy.srtomy.event.EventFixtures.occurrenceOf;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.attendance.domain.EventAttendance;
import org.ouracademy.srtomy.attendance.repository.EventAttendanceRepository;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeleteAttendanceForEventsCommandHandlerImplTests {

  @Mock EventAttendanceRepository eventAttendanceRepository;

  @Test
  void softDeletesTheAttendanceOfEachEvent() {
    var church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);
    var series = eventSeries(church);
    var event = occurrenceOf(series, EventId.generate());
    var attendance = new EventAttendance(event);
    // @MapsId fills the id on persist; a unit test has to set it.
    ReflectionTestUtils.setField(attendance, "id", event.getId());
    var ids = List.of(event.getId());
    when(eventAttendanceRepository.findAllById(ids)).thenReturn(List.of(attendance));

    var handler = new DeleteAttendanceForEventsCommandHandlerImpl(eventAttendanceRepository);
    handler.handle(new DeleteAttendanceForEventsCommand(ids));

    assertThat(attendance.getDeletedTime()).isNotNull();
  }

  @Test
  void eventsWithoutAttendanceAreIgnored() {
    var ids = List.of(EventId.generate());
    when(eventAttendanceRepository.findAllById(ids)).thenReturn(List.of());

    var handler = new DeleteAttendanceForEventsCommandHandlerImpl(eventAttendanceRepository);

    assertThat(handler.handle(new DeleteAttendanceForEventsCommand(ids)))
        .isEqualTo(new DeleteAttendanceForEventsResult());
  }
}
