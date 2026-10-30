package org.ouracademy.ovejafiel.attendance.query.get_attendance_stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.attendance.AttendanceFixtures.eventAttendance;
import static org.ouracademy.ovejafiel.event.EventFixtures.eventBetween;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;
import static org.ouracademy.ovejafiel.person.PersonFixtures.person;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.attendance.repository.EventAttendanceRepository;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.FindEventResult;
import org.ouracademy.ovejafiel.event.query.find_events_in_range.FindEventsInRangeQuery;
import org.ouracademy.ovejafiel.event.query.find_events_in_range.FindEventsInRangeQueryHandler;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.BucketUnit;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

@ExtendWith(MockitoExtension.class)
class GetAttendanceStatsQueryHandlerImplTests {

  @Mock FindEventsInRangeQueryHandler findEventsInRangeQueryHandler;
  @Mock EventAttendanceRepository eventAttendanceRepository;

  GetAttendanceStatsQueryHandlerImpl handler;

  PartyId node = PartyId.generate();
  Organization church;

  @BeforeEach
  void setUp() {
    handler =
        new GetAttendanceStatsQueryHandlerImpl(
            findEventsInRangeQueryHandler, eventAttendanceRepository);
    church = organization(node, "Iglesia", Organization.Type.CHURCH);
  }

  @Test
  void aggregatesEventsAndAttendancePerBucket() {
    var firstId = EventId.generate();
    var secondId = EventId.generate();
    var first =
        eventBetween(
            firstId,
            "Culto",
            church,
            ZonedDateTime.parse("2026-10-01T15:00:00Z"),
            ZonedDateTime.parse("2026-10-01T17:00:00Z"));
    var second =
        eventBetween(
            secondId,
            "Oracion",
            church,
            ZonedDateTime.parse("2026-10-02T15:00:00Z"),
            ZonedDateTime.parse("2026-10-02T16:00:00Z"));
    when(findEventsInRangeQueryHandler.handle(
            new FindEventsInRangeQuery(
                List.of(node),
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-10-02T23:59:59Z"))))
        .thenReturn(List.of(FindEventResult.from(first), FindEventResult.from(second)));
    var ana = person(PartyId.generate(), "Ana", "Diaz");
    var luis = person(PartyId.generate(), "Luis", "Rojas");
    var firstAttendance = eventAttendance(first, List.of(ana, luis));
    firstAttendance.setGuestCount(2);
    var secondAttendance = eventAttendance(second, List.of(ana));
    when(eventAttendanceRepository.findByEventIds(List.of(firstId, secondId)))
        .thenReturn(List.of(firstAttendance, secondAttendance));

    var result =
        handler.handle(
            new GetAttendanceStatsQuery(
                List.of(node),
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-10-02T23:59:59Z"),
                BucketUnit.DAY));

    assertThat(result.buckets()).hasSize(2);
    var dayOne = result.buckets().get(0);
    assertThat(dayOne.label()).isEqualTo("2026-10-01");
    assertThat(dayOne.eventsCount()).isEqualTo(1);
    // 2 people marked present + 2 unnamed guests.
    assertThat(dayOne.attendanceMarks()).isEqualTo(4);
    assertThat(dayOne.uniqueAttendees()).isEqualTo(2);
    assertThat(dayOne.averagePerEvent()).isEqualTo(4.0);
    var dayTwo = result.buckets().get(1);
    assertThat(dayTwo.label()).isEqualTo("2026-10-02");
    assertThat(dayTwo.eventsCount()).isEqualTo(1);
    assertThat(dayTwo.attendanceMarks()).isEqualTo(1);
    assertThat(dayTwo.uniqueAttendees()).isEqualTo(1);
    assertThat(dayTwo.averagePerEvent()).isEqualTo(1.0);
  }

  @Test
  void aBucketWithoutEventsHasZeroesAndNoDivisionByZero() {
    when(findEventsInRangeQueryHandler.handle(any())).thenReturn(List.of());

    var result =
        handler.handle(
            new GetAttendanceStatsQuery(
                List.of(node),
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-10-02T23:59:59Z"),
                BucketUnit.DAY));

    assertThat(result.buckets()).hasSize(2);
    assertThat(result.buckets())
        .allSatisfy(
            bucket -> {
              assertThat(bucket.eventsCount()).isZero();
              assertThat(bucket.attendanceMarks()).isZero();
              assertThat(bucket.uniqueAttendees()).isZero();
              assertThat(bucket.averagePerEvent()).isZero();
            });
    verifyNoInteractions(eventAttendanceRepository);
  }

  @Test
  void startAfterEndIsRejected() {
    assertThatThrownBy(
            () ->
                handler.handle(
                    new GetAttendanceStatsQuery(
                        List.of(node),
                        Instant.parse("2026-10-05T00:00:00Z"),
                        Instant.parse("2026-10-01T00:00:00Z"),
                        BucketUnit.DAY)))
        .isInstanceOf(DomainValidationException.class);
  }
}
