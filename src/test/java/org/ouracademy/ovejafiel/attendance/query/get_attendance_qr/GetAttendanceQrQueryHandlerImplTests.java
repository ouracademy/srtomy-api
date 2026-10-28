package org.ouracademy.ovejafiel.attendance.query.get_attendance_qr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.attendance.query.AttendanceQrToken;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.FindEventResult;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQuery;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQueryHandler;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class GetAttendanceQrQueryHandlerImplTests {

  AttendanceQrToken token = new AttendanceQrToken("a-long-enough-test-secret");
  FindEventByIdQueryHandler events = mock(FindEventByIdQueryHandler.class);
  GetAttendanceQrQueryHandlerImpl handler = new GetAttendanceQrQueryHandlerImpl(token, events);

  EventId eventId = EventId.generate();

  @Test
  void givesACodeForTheEventAndWhenToAskForAnother() {
    var event =
        singleEvent(
            eventId,
            "Culto",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            List.of());
    when(events.handle(new FindEventByIdQuery(eventId)))
        .thenReturn(Optional.of(FindEventResult.from(event)));

    var result = handler.handle(new GetAttendanceQrQuery(eventId));

    var verified = token.verify(result.token(), Instant.now()).orElseThrow();
    assertThat(verified.eventId()).isEqualTo(eventId);
    assertThat(verified.fresh()).isTrue();
    assertThat(result.refreshAt()).isAfter(Instant.now());
  }

  @Test
  void anUnknownEventIsNotFound() {
    when(events.handle(new FindEventByIdQuery(eventId))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new GetAttendanceQrQuery(eventId)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
