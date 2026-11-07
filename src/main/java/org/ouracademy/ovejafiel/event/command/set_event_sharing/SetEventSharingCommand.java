package org.ouracademy.ovejafiel.event.command.set_event_sharing;

import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;

/** {@code applyToProgram}: also give the same access to every sub-event of the event. */
public record SetEventSharingCommand(
    EventId eventId, GeneralAccess generalAccess, boolean applyToProgram) {

  public SetEventSharingCommand(EventId eventId, GeneralAccess generalAccess) {
    this(eventId, generalAccess, false);
  }
}
