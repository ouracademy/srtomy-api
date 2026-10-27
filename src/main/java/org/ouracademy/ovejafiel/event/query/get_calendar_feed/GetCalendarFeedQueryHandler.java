package org.ouracademy.ovejafiel.event.query.get_calendar_feed;

import java.util.Optional;

public interface GetCalendarFeedQueryHandler {
  /** Empty when no feed has that token (never existed, or it was renewed). */
  Optional<GetCalendarFeedResult> handle(GetCalendarFeedQuery query);
}
