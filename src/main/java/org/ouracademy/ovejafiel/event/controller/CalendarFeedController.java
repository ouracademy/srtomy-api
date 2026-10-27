package org.ouracademy.ovejafiel.event.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.event.command.ensure_calendar_feed.EnsureCalendarFeedCommand;
import org.ouracademy.ovejafiel.event.command.ensure_calendar_feed.EnsureCalendarFeedCommandHandler;
import org.ouracademy.ovejafiel.event.command.renew_calendar_feed.RenewCalendarFeedCommand;
import org.ouracademy.ovejafiel.event.command.renew_calendar_feed.RenewCalendarFeedCommandHandler;
import org.ouracademy.ovejafiel.event.controller.dto.CalendarFeedResponse;
import org.ouracademy.ovejafiel.event.query.get_calendar_feed.GetCalendarFeedQuery;
import org.ouracademy.ovejafiel.event.query.get_calendar_feed.GetCalendarFeedQueryHandler;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The private calendar feed: the signed-in user reads (or renews) their address, and calendar apps
 * read the {@code .ics} behind it without credentials other than the token in the path.
 */
@RestController
@AllArgsConstructor
@Tag(name = "calendar-feed")
public class CalendarFeedController {

  private static final String TEXT_CALENDAR = "text/calendar;charset=UTF-8";

  EnsureCalendarFeedCommandHandler ensureCalendarFeedCommandHandler;
  RenewCalendarFeedCommandHandler renewCalendarFeedCommandHandler;
  GetCalendarFeedQueryHandler getCalendarFeedQueryHandler;

  @GetMapping("me/calendar-feed")
  public CalendarFeedResponse myFeed(@AuthenticationPrincipal Jwt jwt) {
    return CalendarFeedResponse.of(
        ensureCalendarFeedCommandHandler
            .handle(new EnsureCalendarFeedCommand(jwt.getSubject()))
            .token());
  }

  /** The old address stops working. */
  @PostMapping("me/calendar-feed/renew")
  public CalendarFeedResponse renew(@AuthenticationPrincipal Jwt jwt) {
    return CalendarFeedResponse.of(
        renewCalendarFeedCommandHandler
            .handle(new RenewCalendarFeedCommand(jwt.getSubject()))
            .token());
  }

  /** 404 for an unknown or renewed token, without saying which. */
  @GetMapping(value = "public/calendar/{token}.ics", produces = TEXT_CALENDAR)
  public ResponseEntity<String> feed(@PathVariable String token) {
    var result =
        getCalendarFeedQueryHandler
            .handle(new GetCalendarFeedQuery(token, Instant.now()))
            .orElseThrow(() -> new NoSuchElementException("Calendar not found"));
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noCache().cachePrivate())
        .body(result.calendar());
  }
}
