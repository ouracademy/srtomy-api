package org.ouracademy.ovejafiel.whatsapp.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AgentPausedRequest(
    @NotNull @Schema(description = "true pauses the assistant, false resumes it") Boolean paused) {}
