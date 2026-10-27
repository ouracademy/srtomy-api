package org.ouracademy.ovejafiel.event.command.renew_calendar_feed;

public interface RenewCalendarFeedCommandHandler {
  /**
   * @throws java.util.NoSuchElementException when the user does not exist
   */
  RenewCalendarFeedResult handle(RenewCalendarFeedCommand command);
}
