package org.ouracademy.srtomy.iam.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RoleRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 1000) String description,
    @NotNull List<String> permissions) {}
