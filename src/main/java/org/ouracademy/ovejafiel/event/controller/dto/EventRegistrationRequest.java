package org.ouracademy.ovejafiel.event.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Someone signing up for an event or course with a ticket. */
public record EventRegistrationRequest(
    @NotNull UUID ticketTypeId,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Email @Size(max = 200) String email,
    @Size(max = 40) String phoneNumber) {}
