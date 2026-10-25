package org.ouracademy.srtomy.event.command.set_event_sharing;

import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.GeneralAccess;
import org.ouracademy.srtomy.event.domain.LinkRole;

public record SetEventSharingCommand(
    EventId eventId, GeneralAccess generalAccess, LinkRole linkRole) {}
