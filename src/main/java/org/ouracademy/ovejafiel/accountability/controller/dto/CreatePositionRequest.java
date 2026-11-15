package org.ouracademy.ovejafiel.accountability.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Both lists are optional: with neither, the position is only a label.
 *
 * @param name Name of the position (cargo), such as Pastor or Usher.
 * @param roleIds Ids of access levels, from the roles of the church. Optional.
 * @param permissions Loose permission values, such as {@code members.read}; the full list is in
 *     {@code GET /v1/iam/permissions}. Optional.
 */
public record CreatePositionRequest(
    @NotBlank @Size(max = 255) String name, List<UUID> roleIds, List<String> permissions) {}
