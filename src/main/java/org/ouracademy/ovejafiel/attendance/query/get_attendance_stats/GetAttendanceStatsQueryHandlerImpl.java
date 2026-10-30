package org.ouracademy.ovejafiel.attendance.query.get_attendance_stats;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.attendance.domain.EventAttendance;
import org.ouracademy.ovejafiel.attendance.query.get_attendance_stats.GetAttendanceStatsResult.Bucket;
import org.ouracademy.ovejafiel.attendance.repository.EventAttendanceRepository;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.FindEventResult;
import org.ouracademy.ovejafiel.event.query.find_events_in_range.FindEventsInRangeQuery;
import org.ouracademy.ovejafiel.event.query.find_events_in_range.FindEventsInRangeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.BucketUnit.Period;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Events of the range (through the event module's own query, never its repository) plus their
 * attendance sheets, aggregated per period: how many events happened, how many marks (people plus
 * unnamed guests) and how many distinct people attended.
 */
@Service
@RequiredArgsConstructor
class GetAttendanceStatsQueryHandlerImpl implements GetAttendanceStatsQueryHandler {

  private final FindEventsInRangeQueryHandler findEventsInRangeQueryHandler;
  private final EventAttendanceRepository eventAttendanceRepository;

  @Override
  @Transactional(readOnly = true)
  public GetAttendanceStatsResult handle(GetAttendanceStatsQuery query) {
    if (query.start().isAfter(query.end())) {
      throw new DomainValidationException("La fecha de inicio no puede ser posterior a la de fin");
    }

    var startLdt = LocalDateTime.ofInstant(query.start(), ZoneOffset.UTC);
    var endLdt = LocalDateTime.ofInstant(query.end(), ZoneOffset.UTC);
    var periods = query.bucket().bucketsOver(startLdt, endLdt);

    var events =
        findEventsInRangeQueryHandler.handle(
            new FindEventsInRangeQuery(query.organizationIds(), query.start(), query.end()));
    var attendanceByEventId = attendanceByEventId(events);

    var tallies = new ArrayList<Tally>(periods.size());
    for (var i = 0; i < periods.size(); i++) {
      tallies.add(new Tally());
    }

    for (var event : events) {
      var at = event.start().withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
      var index = indexOf(periods, at);
      if (index < 0) {
        continue;
      }
      var tally = tallies.get(index);
      tally.eventsCount++;
      var attendance = attendanceByEventId.get(event.id());
      if (attendance != null) {
        tally.attendanceMarks += attendance.getPersonIds().size() + attendance.getGuestCount();
        tally.attendees.addAll(attendance.getPersonIds());
      }
    }

    var buckets = new ArrayList<Bucket>(periods.size());
    for (var i = 0; i < periods.size(); i++) {
      buckets.add(toBucket(periods.get(i), tallies.get(i)));
    }
    return new GetAttendanceStatsResult(buckets);
  }

  private Map<EventId, EventAttendance> attendanceByEventId(List<FindEventResult> events) {
    if (events.isEmpty()) {
      return Map.of();
    }
    return eventAttendanceRepository
        .findByEventIds(events.stream().map(FindEventResult::id).toList())
        .stream()
        .collect(Collectors.toMap(EventAttendance::getId, attendance -> attendance));
  }

  private static int indexOf(List<Period> periods, LocalDateTime at) {
    for (var i = 0; i < periods.size(); i++) {
      var period = periods.get(i);
      if (!at.isBefore(period.start()) && at.isBefore(period.endExclusive())) {
        return i;
      }
    }
    return -1;
  }

  private static Bucket toBucket(Period period, Tally tally) {
    var average = tally.eventsCount == 0 ? 0.0 : (double) tally.attendanceMarks / tally.eventsCount;
    return new Bucket(
        period.start().toLocalDate(),
        period.endExclusive().minusDays(1).toLocalDate(),
        period.label(),
        tally.eventsCount,
        tally.attendanceMarks,
        tally.attendees.size(),
        average);
  }

  /** What happened in one period, before it is turned into its {@code Bucket}. */
  private static final class Tally {
    long eventsCount;
    long attendanceMarks;
    final Set<PartyId> attendees = new HashSet<>();
  }
}
