package org.ouracademy.ovejafiel.attendance.command.set_attendance_guests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.attendance.AttendanceFixtures.eventAttendance;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.attendance.repository.EventAttendanceRepository;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.FindEventResult;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQuery;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQueryHandler;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class SetAttendanceGuestsCommandHandlerImplTests {

  EventAttendanceRepository attendances = mock(EventAttendanceRepository.class);
  FindEventByIdQueryHandler events = mock(FindEventByIdQueryHandler.class);
  SetAttendanceGuestsCommandHandlerImpl handler =
      new SetAttendanceGuestsCommandHandlerImpl(attendances, events);

  EventId eventId = EventId.generate();
  Event event =
      singleEvent(
          eventId,
          "Culto",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @Test
  void setsHowManyVisitorsWithoutANameCame() {
    var attendance = eventAttendance(event, List.of());
    when(events.handle(new FindEventByIdQuery(eventId)))
        .thenReturn(Optional.of(FindEventResult.from(event)));
    when(attendances.findByEventId(eventId)).thenReturn(Optional.of(attendance));

    var result = handler.handle(new SetAttendanceGuestsCommand(eventId, 5));

    assertThat(result.guestCount()).isEqualTo(5);
    assertThat(attendance.getGuestCount()).isEqualTo(5);
    verify(attendances).createIfMissing(eventId.value());
    verify(attendances).save(attendance);
  }

  @Test
  void aNegativeCountIsRefused() {
    var attendance = eventAttendance(event, List.of());
    when(events.handle(new FindEventByIdQuery(eventId)))
        .thenReturn(Optional.of(FindEventResult.from(event)));
    when(attendances.findByEventId(eventId)).thenReturn(Optional.of(attendance));

    assertThatThrownBy(() -> handler.handle(new SetAttendanceGuestsCommand(eventId, -1)))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void anUnknownEventIsNotFound() {
    when(events.handle(new FindEventByIdQuery(eventId))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new SetAttendanceGuestsCommand(eventId, 2)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
