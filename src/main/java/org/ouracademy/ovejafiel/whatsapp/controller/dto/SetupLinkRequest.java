package org.ouracademy.ovejafiel.whatsapp.controller.dto;

import jakarta.validation.constraints.NotNull;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;

public record SetupLinkRequest(@NotNull ConnectionMode mode) {}
