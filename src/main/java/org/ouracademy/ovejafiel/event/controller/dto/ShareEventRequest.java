package org.ouracademy.ovejafiel.event.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.ouracademy.ovejafiel.event.domain.ShareRole;

public record ShareEventRequest(@NotBlank @Email String email, @NotNull ShareRole role) {}
