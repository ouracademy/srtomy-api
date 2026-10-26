package org.ouracademy.ovejafiel.iam.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OwnerRequest(@NotBlank @Email String email) {}
