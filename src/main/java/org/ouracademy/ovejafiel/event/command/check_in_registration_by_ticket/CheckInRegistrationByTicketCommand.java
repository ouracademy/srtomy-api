package org.ouracademy.ovejafiel.event.command.check_in_registration_by_ticket;

import org.ouracademy.ovejafiel.event.domain.EventId;

/** An usher scanned the ticket QR whose code is {@code ticketCode}. */
public record CheckInRegistrationByTicketCommand(EventId eventId, String ticketCode) {}
