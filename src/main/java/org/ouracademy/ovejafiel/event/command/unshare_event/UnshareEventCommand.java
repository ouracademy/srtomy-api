package org.ouracademy.ovejafiel.event.command.unshare_event;

import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventShareId;

public record UnshareEventCommand(EventId eventId, EventShareId shareId) {}
