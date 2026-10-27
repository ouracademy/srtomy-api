package org.ouracademy.ovejafiel.event.command.remove_event_registration;

import org.ouracademy.ovejafiel.event.domain.EventId;

public record RemoveEventRegistrationCommand(EventId eventId) {}
