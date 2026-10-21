package org.ouracademy.srtomy.event.command;

import org.ouracademy.srtomy.event.domain.EventChangeOption;
import org.ouracademy.srtomy.event.domain.EventId;

/**
 * Edits an event and, depending on {@code option}, the rest of its recurrence. Not implemented yet
 * (see {@link EditEventCommandHandlerImpl}); the new event data will be added here when it is.
 */
public record EditEventCommand(EventId id, EventChangeOption option) {}
