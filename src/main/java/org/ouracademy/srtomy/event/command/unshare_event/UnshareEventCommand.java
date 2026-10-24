package org.ouracademy.srtomy.event.command.unshare_event;

import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.EventShareId;

public record UnshareEventCommand(EventId eventId, EventShareId shareId) {}
