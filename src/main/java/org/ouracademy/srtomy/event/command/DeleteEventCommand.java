package org.ouracademy.srtomy.event.command;

import org.ouracademy.srtomy.event.domain.EventChangeOption;
import org.ouracademy.srtomy.event.domain.EventId;

/** Deletes a single event and, depending on {@code option}, the rest of its recurrence. */
public record DeleteEventCommand(EventId id, EventChangeOption option) {}
