package org.ouracademy.ovejafiel.event.command.ensure_calendar_feed;

/** Gives the user identified by {@code subject} a calendar feed, if they have none yet. */
public record EnsureCalendarFeedCommand(String subject) {}
