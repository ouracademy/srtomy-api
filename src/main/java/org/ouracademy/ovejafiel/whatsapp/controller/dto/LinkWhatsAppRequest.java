package org.ouracademy.ovejafiel.whatsapp.controller.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code code} is whatever the person typed after the keyword; it is normalized here. */
public record LinkWhatsAppRequest(
    @NotBlank String phoneNumberId, @NotBlank String from, @NotBlank String code) {}
