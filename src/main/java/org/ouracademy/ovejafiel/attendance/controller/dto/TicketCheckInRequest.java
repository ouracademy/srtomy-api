package org.ouracademy.ovejafiel.attendance.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * The code of the ticket QR an usher scanned.
 *
 * @param ticketCode The code read from the QR of the ticket.
 */
public record TicketCheckInRequest(@NotBlank String ticketCode) {}
