package org.ouracademy.srtomy.accountability.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PositionRequest(@NotBlank @Size(max = 255) String name) {}
