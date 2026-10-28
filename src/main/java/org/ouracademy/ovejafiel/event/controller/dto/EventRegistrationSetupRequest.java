package org.ouracademy.ovejafiel.event.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * How the event takes sign-ups. {@code tickets} is the whole list: one with an {@code id} updates
 * that ticket, one without creates it, one left out is removed. The seats are the quantity of each
 * ticket (null: unlimited) and the price is informational. Closing the registration is another
 * call.
 */
public record EventRegistrationSetupRequest(
    boolean requiresAccount, @NotEmpty @Valid List<TicketRequest> tickets) {

  public record TicketRequest(
      UUID id,
      @NotBlank @Size(max = 100) String name,
      @Min(0) int priceCents,
      @Size(max = 3) String currency,
      @Min(1) Integer quantity,
      ZonedDateTime salesStart,
      ZonedDateTime salesEnd) {}
}
