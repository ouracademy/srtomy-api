package org.ouracademy.ovejafiel.event.command.set_registration_check_in;

import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;

/** A pastor or usher marks a sign-up as present, or takes the mark back. */
public record SetRegistrationCheckInCommand(
    EventId eventId, EventRegistrationId registrationId, boolean checkedIn) {}
