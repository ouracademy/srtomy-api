package org.ouracademy.ovejafiel.event.command.copy_event;

import org.ouracademy.ovejafiel.event.domain.EventId;

/** The copy, and how many sub-events were copied with it. */
public record CopyEventResult(EventId id, int subEvents) {}
