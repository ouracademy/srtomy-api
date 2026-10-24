package org.ouracademy.srtomy.accountability.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** {@code accessRoleId} is optional: without it the position is only a label. */
public record CreatePositionRequest(@NotBlank @Size(max = 255) String name, UUID accessRoleId) {}
