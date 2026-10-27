package org.ouracademy.ovejafiel.event.command.check_in_user_registration;

import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.user.domain.UserId;

/**
 * Someone scanned the QR of the event: if the account {@code userId}, or its verified {@code
 * email}, holds a seat, that sign-up is checked in.
 */
public record CheckInUserRegistrationCommand(EventId eventId, UserId userId, String email) {}
