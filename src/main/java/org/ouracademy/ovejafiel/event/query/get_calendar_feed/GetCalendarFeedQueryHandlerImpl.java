package org.ouracademy.ovejafiel.event.query.get_calendar_feed;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.repository.CalendarFeedRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The events of every church the user can read events in, from a month ago to a year ahead: one
 * entry per occurrence, so deleting one occurrence needs no exception rule in the calendar apps.
 */
@Service
@RequiredArgsConstructor
class GetCalendarFeedQueryHandlerImpl implements GetCalendarFeedQueryHandler {

  static final Duration PAST = Duration.ofDays(30);
  static final Duration FUTURE = Duration.ofDays(366);
  static final int MAX_EVENTS = 5000;

  private final CalendarFeedRepository calendarFeedRepository;
  private final EventRepository eventRepository;

  @Value("${app.url:http://localhost:3000}")
  private String appUrl;

  @Override
  @Transactional(readOnly = true)
  public Optional<GetCalendarFeedResult> handle(GetCalendarFeedQuery query) {
    return calendarFeedRepository
        .findByToken(query.token())
        .map(
            feed -> {
              var events =
                  eventRepository.findReadableByUser(
                      feed.getUserId().value(),
                      query.now().minus(PAST),
                      query.now().plus(FUTURE),
                      MAX_EVENTS);
              var entries = events.stream().map(this::entry).toList();
              return new GetCalendarFeedResult(
                  IcsCalendar.write("Sr. Tommy: eventos", entries, query.now()));
            });
  }

  private IcsCalendar.Entry entry(Event event) {
    var organization = event.getOrganization();
    return new IcsCalendar.Entry(
        event.getId().value() + "@srtomy",
        event.getStart().toInstant(),
        event.getEnd().toInstant(),
        event.getTitle() + " · " + organization.getName(),
        event.getAddress(),
        event.getDescription(),
        appUrl.replaceAll("/+$", "")
            + "/portal/"
            + organization.getId().value()
            + "/event/"
            + event.getId().value()
            + "/attendance");
  }
}
