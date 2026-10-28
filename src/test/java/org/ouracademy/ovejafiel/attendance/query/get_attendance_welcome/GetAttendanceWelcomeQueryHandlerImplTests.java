package org.ouracademy.ovejafiel.attendance.query.get_attendance_welcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.eventBetween;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.attendance.query.AttendanceQrToken;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_welcome.GetAttendanceWelcomeResult.Status;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.FindEventResult;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQuery;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQueryHandler;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class GetAttendanceWelcomeQueryHandlerImplTests {

  AttendanceQrToken token = new AttendanceQrToken("a-long-enough-test-secret");
  FindEventByIdQueryHandler events = mock(FindEventByIdQueryHandler.class);
  GetAttendanceWelcomeQueryHandlerImpl handler =
      new GetAttendanceWelcomeQueryHandlerImpl(token, events);

  EventId eventId = EventId.generate();

  void eventAt(ZonedDateTime start, ZonedDateTime end) {
    var event =
        eventBetween(
            eventId,
            "Culto dominical",
            organization(PartyId.generate(), "Iglesia Central", Organization.Type.CHURCH),
            start,
            end);
    when(events.handle(new FindEventByIdQuery(eventId)))
        .thenReturn(Optional.of(FindEventResult.from(event)));
  }

  @Test
  void showsTheEventAndSaysItIsOpenWhileItIsOn() {
    eventAt(ZonedDateTime.now().minusMinutes(10), ZonedDateTime.now().plusHours(1));

    var result =
        handler
            .handle(new GetAttendanceWelcomeQuery(token.issue(eventId, Instant.now())))
            .orElseThrow();

    assertThat(result.title()).isEqualTo("Culto dominical");
    assertThat(result.organizationName()).isEqualTo("Iglesia Central");
    assertThat(result.status()).isEqualTo(Status.OPEN);
  }

  @Test
  void anOldCodeIsExpired() {
    eventAt(ZonedDateTime.now().minusMinutes(10), ZonedDateTime.now().plusHours(1));

    var result =
        handler
            .handle(
                new GetAttendanceWelcomeQuery(
                    token.issue(eventId, Instant.now().minusSeconds(3600))))
            .orElseThrow();

    assertThat(result.status()).isEqualTo(Status.EXPIRED_CODE);
  }

  @Test
  void aCodeOutsideTheWindowOfTheEventIsOutOfWindow() {
    eventAt(ZonedDateTime.now().plusDays(3), ZonedDateTime.now().plusDays(3).plusHours(2));

    var result =
        handler
            .handle(new GetAttendanceWelcomeQuery(token.issue(eventId, Instant.now())))
            .orElseThrow();

    assertThat(result.status()).isEqualTo(Status.OUT_OF_WINDOW);
  }

  @Test
  void anAlteredCodeOrAMissingEventIsEmpty() {
    assertThat(handler.handle(new GetAttendanceWelcomeQuery("garbage"))).isEmpty();

    when(events.handle(new FindEventByIdQuery(eventId))).thenReturn(Optional.empty());
    assertThat(handler.handle(new GetAttendanceWelcomeQuery(token.issue(eventId, Instant.now()))))
        .isEmpty();
  }
}
