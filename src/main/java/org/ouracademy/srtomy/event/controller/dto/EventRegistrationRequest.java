package org.ouracademy.srtomy.event.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Someone signing up for a course through its link. */
public record EventRegistrationRequest(
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Email @Size(max = 200) String email,
    @Size(max = 40) String phoneNumber) {}
