package org.ouracademy.srtomy.event.query.get_calendar_feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.event.EventFixtures.singleEvent;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.event.domain.CalendarFeed;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.repository.CalendarFeedRepository;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.domain.UserId;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GetCalendarFeedQueryHandlerImplTests {

  static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  @Mock CalendarFeedRepository calendarFeedRepository;
  @Mock EventRepository eventRepository;

  GetCalendarFeedQueryHandlerImpl handler;
  UserId userId = UserId.generate();

  @BeforeEach
  void setUp() {
    handler = new GetCalendarFeedQueryHandlerImpl(calendarFeedRepository, eventRepository);
    ReflectionTestUtils.setField(handler, "appUrl", "https://app.example/");
  }

  @Test
  void writesOneEntryPerEventTheUserCanRead() {
    var feed = new CalendarFeed(userId);
    var church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);
    var event = singleEvent(EventId.generate(), "Culto", church, List.of());
    when(calendarFeedRepository.findByToken("tok")).thenReturn(Optional.of(feed));
    when(eventRepository.findReadableByUser(
            eq(userId.value()),
            eq(NOW.minus(GetCalendarFeedQueryHandlerImpl.PAST)),
            eq(NOW.plus(GetCalendarFeedQueryHandlerImpl.FUTURE)),
            eq(GetCalendarFeedQueryHandlerImpl.MAX_EVENTS)))
        .thenReturn(List.of(event));

    // Long lines are folded: unfold them to compare.
    var calendar =
        handler
            .handle(new GetCalendarFeedQuery("tok", NOW))
            .orElseThrow()
            .calendar()
            .replace("\r\n ", "");

    assertThat(calendar)
        .contains(
            "UID:" + event.getId().value() + "@srtomy\r\n",
            "SUMMARY:Culto · Iglesia\r\n",
            "LOCATION:Av. Siempre Viva 123\r\n",
            "DTSTART:20260105T150000Z\r\n",
            "URL:https://app.example/portal/"
                + church.getId().value()
                + "/event/"
                + event.getId().value()
                + "/attendance\r\n");
  }

  @Test
  void isEmptyForAnUnknownToken() {
    when(calendarFeedRepository.findByToken("nope")).thenReturn(Optional.empty());

    assertThat(handler.handle(new GetCalendarFeedQuery("nope", NOW))).isEmpty();
  }
}
