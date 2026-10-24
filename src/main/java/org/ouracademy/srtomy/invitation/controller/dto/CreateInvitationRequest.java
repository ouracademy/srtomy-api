package org.ouracademy.srtomy.invitation.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * {@code personId} links an existing person of the node; without it {@code firstName} and {@code
 * lastName} name the person that is created when the invitation is accepted. {@code positionId}
 * (optional) is the position they take.
 */
public record CreateInvitationRequest(
    @NotBlank @Email String email,
    UUID positionId,
    UUID personId,
    String firstName,
    String lastName) {}
