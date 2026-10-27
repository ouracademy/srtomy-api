package org.ouracademy.ovejafiel.event.command.set_event_sharing;

import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;

public record SetEventSharingCommand(EventId eventId, GeneralAccess generalAccess) {}
