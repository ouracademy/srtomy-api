package org.ouracademy.srtomy.complaint.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Body of the public complaint form; same fields and constraints as the {@code Complaint}. */
public record CreateComplaintRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    // Optional: not every complaint comes with an identity document (e.g. anonymous
    // complaints). When present, it must still look like a real DNI.
    @Pattern(regexp = "^\\d{8}$") String dni,
    String address,
    @NotBlank @Email String email,
    @NotBlank String comment) {}
