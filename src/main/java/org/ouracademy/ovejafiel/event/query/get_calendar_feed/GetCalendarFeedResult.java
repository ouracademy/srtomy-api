package org.ouracademy.ovejafiel.event.query.get_calendar_feed;

/** The calendar in iCalendar format (RFC 5545), ready to serve as {@code text/calendar}. */
public record GetCalendarFeedResult(String calendar) {}
