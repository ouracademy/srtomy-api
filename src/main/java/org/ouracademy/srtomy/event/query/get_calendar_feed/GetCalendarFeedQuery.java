package org.ouracademy.srtomy.event.query.get_calendar_feed;

import java.time.Instant;

/** The calendar behind the feed {@code token}, with the events around {@code now}. */
public record GetCalendarFeedQuery(String token, Instant now) {}
