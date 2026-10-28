package org.ouracademy.ovejafiel.event.command.register_for_event;

import org.ouracademy.ovejafiel.event.domain.TicketTypeId;

/**
 * Signs up for the event whose shared link is {@code slug}, with the ticket {@code ticketTypeId};
 * {@code subject} is null if anonymous.
 */
public record RegisterForEventCommand(
    String slug,
    String subject,
    TicketTypeId ticketTypeId,
    String firstName,
    String lastName,
    String email,
    String phoneNumber) {}
