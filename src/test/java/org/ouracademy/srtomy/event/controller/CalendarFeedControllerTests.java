package org.ouracademy.srtomy.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.event.command.ensure_calendar_feed.EnsureCalendarFeedCommand;
import org.ouracademy.srtomy.event.command.ensure_calendar_feed.EnsureCalendarFeedCommandHandler;
import org.ouracademy.srtomy.event.command.ensure_calendar_feed.EnsureCalendarFeedResult;
import org.ouracademy.srtomy.event.command.renew_calendar_feed.RenewCalendarFeedCommand;
import org.ouracademy.srtomy.event.command.renew_calendar_feed.RenewCalendarFeedCommandHandler;
import org.ouracademy.srtomy.event.command.renew_calendar_feed.RenewCalendarFeedResult;
import org.ouracademy.srtomy.event.query.get_calendar_feed.GetCalendarFeedQueryHandler;
import org.ouracademy.srtomy.event.query.get_calendar_feed.GetCalendarFeedResult;
import org.ouracademy.srtomy.shared.SecurityConfig;
import org.ouracademy.srtomy.shared.SrtomyApplication;
import org.ouracademy.srtomy.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(CalendarFeedController.class)
@ContextConfiguration(classes = SrtomyApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class CalendarFeedControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean EnsureCalendarFeedCommandHandler ensureCalendarFeedCommandHandler;
  @MockitoBean RenewCalendarFeedCommandHandler renewCalendarFeedCommandHandler;
  @MockitoBean GetCalendarFeedQueryHandler getCalendarFeedQueryHandler;

  private String bearer() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder, "auth0|ana");
  }

  @Test
  void myFeedReturnsTheAddressOfTheSignedInUser() {
    given(ensureCalendarFeedCommandHandler.handle(any()))
        .willReturn(new EnsureCalendarFeedResult("tok123"));

    assertThat(mvc.get().uri("/v1/me/calendar-feed").header("Authorization", bearer()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.path")
        .isEqualTo("/v1/public/calendar/tok123.ics");
    verify(ensureCalendarFeedCommandHandler).handle(new EnsureCalendarFeedCommand("auth0|ana"));
  }

  @Test
  void renewReturnsTheNewAddress() {
    given(renewCalendarFeedCommandHandler.handle(any()))
        .willReturn(new RenewCalendarFeedResult("new456"));

    assertThat(mvc.post().uri("/v1/me/calendar-feed/renew").header("Authorization", bearer()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.path")
        .isEqualTo("/v1/public/calendar/new456.ics");
    verify(renewCalendarFeedCommandHandler).handle(new RenewCalendarFeedCommand("auth0|ana"));
  }

  @Test
  void myFeedNeedsASession() {
    assertThat(mvc.get().uri("/v1/me/calendar-feed")).hasStatus(401);
  }

  @Test
  void theCalendarIsServedWithoutASessionAsTextCalendar() {
    given(getCalendarFeedQueryHandler.handle(any()))
        .willReturn(Optional.of(new GetCalendarFeedResult("BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n")));

    var response = mvc.get().uri("/v1/public/calendar/tok123.ics");

    assertThat(response).hasStatusOk();
    assertThat(response).headers().hasValue("Content-Type", "text/calendar;charset=UTF-8");
    assertThat(response).bodyText().startsWith("BEGIN:VCALENDAR");
  }

  @Test
  void anUnknownTokenIsNotFound() {
    given(getCalendarFeedQueryHandler.handle(any())).willReturn(Optional.empty());

    assertThat(mvc.get().uri("/v1/public/calendar/nope.ics")).hasStatus(404);
  }
}
