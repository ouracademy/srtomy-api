package org.ouracademy.ovejafiel.attendance.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** The code of the ticket QR an usher scanned. */
public record TicketCheckInRequest(@NotBlank String ticketCode) {}
