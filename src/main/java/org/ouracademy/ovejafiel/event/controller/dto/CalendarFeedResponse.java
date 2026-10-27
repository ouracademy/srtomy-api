package org.ouracademy.ovejafiel.event.controller.dto;

/**
 * The address of the calendar feed, relative to the API origin: subscribe to it from Google
 * Calendar or Apple Calendar.
 */
public record CalendarFeedResponse(String path) {

  public static CalendarFeedResponse of(String token) {
    return new CalendarFeedResponse("/v1/public/calendar/" + token + ".ics");
  }
}
