package org.ouracademy.ovejafiel.event.command.cancel_event_registration;

import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;

public record CancelEventRegistrationCommand(EventId eventId, EventRegistrationId registrationId) {}
