package org.ouracademy.srtomy.iam.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GrantAccessRequest(@NotBlank String email, @NotNull UUID roleId) {}
