package org.ouracademy.ovejafiel.event.command.edit_event;

import org.ouracademy.ovejafiel.event.domain.EventId;

/**
 * {@code keptEvents}: occurrences with sign-ups, attendance or already past that a change of the
 * rule left untouched, as one-off events. {@code notifiedPeople}: people emailed about a new time.
 */
public record EditEventResult(EventId id, int keptEvents, int notifiedPeople) {}
