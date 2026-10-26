package org.ouracademy.ovejafiel.event.command.delete_event;

import org.ouracademy.ovejafiel.event.domain.EventChangeOption;
import org.ouracademy.ovejafiel.event.domain.EventId;

/** Deletes a single event and, depending on {@code option}, the rest of its recurrence. */
public record DeleteEventCommand(EventId id, EventChangeOption option) {}
