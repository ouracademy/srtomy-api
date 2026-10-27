package org.ouracademy.ovejafiel.event.command.edit_event;

import org.ouracademy.ovejafiel.event.domain.EventChangeOption;
import org.ouracademy.ovejafiel.event.domain.EventId;

/**
 * Edits an event and, depending on {@code option}, the rest of its recurrence. Not implemented yet
 * (see {@link EditEventCommandHandlerImpl}); the new event data will be added here when it is.
 */
public record EditEventCommand(EventId id, EventChangeOption option) {}
