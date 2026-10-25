package org.ouracademy.srtomy.event.command.cancel_event_registration;

import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.EventRegistrationId;

public record CancelEventRegistrationCommand(EventId eventId, EventRegistrationId registrationId) {}
