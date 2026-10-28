package org.ouracademy.ovejafiel.event.command.set_registration_open;

import org.ouracademy.ovejafiel.event.domain.EventId;

public record SetRegistrationOpenCommand(EventId eventId, boolean open) {}
