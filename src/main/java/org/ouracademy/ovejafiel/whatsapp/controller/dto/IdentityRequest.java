package org.ouracademy.ovejafiel.whatsapp.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code phoneNumberId}: the church number written to; {@code from}: who wrote. */
public record IdentityRequest(@NotBlank String phoneNumberId, @NotBlank String from) {}
