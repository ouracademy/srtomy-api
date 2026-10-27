package org.ouracademy.ovejafiel.event.command.ensure_calendar_feed;

public interface EnsureCalendarFeedCommandHandler {
  /**
   * @throws java.util.NoSuchElementException when the user does not exist
   */
  EnsureCalendarFeedResult handle(EnsureCalendarFeedCommand command);
}
