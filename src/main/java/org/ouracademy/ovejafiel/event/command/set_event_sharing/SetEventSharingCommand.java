package org.ouracademy.ovejafiel.event.command.set_event_sharing;

import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;
import org.ouracademy.ovejafiel.event.domain.LinkRole;

public record SetEventSharingCommand(
    EventId eventId, GeneralAccess generalAccess, LinkRole linkRole) {}
