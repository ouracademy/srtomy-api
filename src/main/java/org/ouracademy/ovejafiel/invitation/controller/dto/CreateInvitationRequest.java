package org.ouracademy.ovejafiel.invitation.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * Invites {@code email}. {@code personId} links an existing person of the node (who gets that email
 * if they have none); without it the only person of the node with that email is linked, and
 * otherwise a person is created when the invitation is accepted, named by {@code firstName} and
 * {@code lastName} if given or by whoever accepts. {@code positionId} (optional) is the position
 * they take.
 *
 * @param email Email the invitation is sent to.
 * @param positionId Id of the position (cargo) they take. Optional.
 * @param personId Id of an existing person of the node to link the account to. Optional.
 * @param firstName First names for the person to create, when none is linked. Optional.
 * @param lastName Last names for the person to create, when none is linked. Optional.
 */
public record CreateInvitationRequest(
    @NotBlank @Email String email,
    UUID positionId,
    UUID personId,
    String firstName,
    String lastName) {}
