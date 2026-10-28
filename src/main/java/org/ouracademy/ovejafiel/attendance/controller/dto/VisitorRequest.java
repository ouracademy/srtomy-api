package org.ouracademy.ovejafiel.attendance.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A new visitor: only the first name is required. */
public record VisitorRequest(
    @NotBlank @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @Size(max = 40) String phoneNumber,
    @Email @Size(max = 200) String email) {}
