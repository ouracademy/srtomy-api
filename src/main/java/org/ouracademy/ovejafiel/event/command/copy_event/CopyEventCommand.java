package org.ouracademy.ovejafiel.event.command.copy_event;

import java.time.OffsetDateTime;
import org.ouracademy.ovejafiel.event.domain.EventId;

/**
 * Copy an event, with its program when it is a mega event, so that it starts at {@code newStart}.
 * Every sub-event keeps its place relative to the start. {@code name} is optional.
 */
public record CopyEventCommand(EventId eventId, OffsetDateTime newStart, String name) {}
