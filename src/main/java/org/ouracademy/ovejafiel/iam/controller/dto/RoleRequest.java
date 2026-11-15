package org.ouracademy.ovejafiel.iam.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * An access level of a church: a named set of permissions.
 *
 * @param name Name of the access level, as the church calls it.
 * @param description What the level is for. Optional.
 * @param permissions Permission values, such as {@code members.read}; the full list is in {@code
 *     GET /v1/iam/permissions}.
 */
public record RoleRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 1000) String description,
    @NotNull List<String> permissions) {}
