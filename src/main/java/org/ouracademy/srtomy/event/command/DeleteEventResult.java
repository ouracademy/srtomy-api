package org.ouracademy.srtomy.event.command;

import org.ouracademy.srtomy.event.domain.EventId;

/** {@code id} is the single event the deletion was requested for. */
public record DeleteEventResult(EventId id) {}
