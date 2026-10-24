package org.ouracademy.srtomy.accountability.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Both lists are optional: with neither, the position is only a label. */
public record CreatePositionRequest(
    @NotBlank @Size(max = 255) String name, List<UUID> roleIds, List<String> permissions) {}
