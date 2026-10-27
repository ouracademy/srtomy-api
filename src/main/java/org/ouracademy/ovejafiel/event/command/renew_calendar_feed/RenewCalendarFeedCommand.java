package org.ouracademy.ovejafiel.event.command.renew_calendar_feed;

/** Revokes the feed address of the user identified by {@code subject} and gives them a new one. */
public record RenewCalendarFeedCommand(String subject) {}
