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
 *
 * @param requiresAccount true when only signed-in users can sign up.
 * @param tickets The complete list of tickets of the event; at least one.
 */
public record EventRegistrationSetupRequest(
    boolean requiresAccount, @NotEmpty @Valid List<TicketRequest> tickets) {

  /**
   * A kind of ticket of an event.
   *
   * @param id Id of an existing ticket to update; omit it to create a new one.
   * @param name Name of the ticket, such as General or Students.
   * @param priceCents Price in cents (1500 is 15.00); 0 when free. Informational only: nothing is
   *     charged.
   * @param currency Currency code of three letters, such as PEN. Optional.
   * @param quantity How many seats there are; omit for unlimited.
   * @param salesStart When sign-ups with this ticket start. Optional.
   * @param salesEnd When sign-ups with this ticket end. Optional.
   * @param subEventIds Mega events only: ids of the sub-events this ticket lets in. Optional.
   */
  public record TicketRequest(
      UUID id,
      @NotBlank @Size(max = 100) String name,
      @Min(0) int priceCents,
      @Size(max = 3) String currency,
      @Min(1) Integer quantity,
      ZonedDateTime salesStart,
      ZonedDateTime salesEnd,
      List<UUID> subEventIds) {}
}
