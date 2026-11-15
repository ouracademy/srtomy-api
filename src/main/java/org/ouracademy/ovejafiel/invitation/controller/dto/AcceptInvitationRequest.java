package org.ouracademy.ovejafiel.invitation.controller.dto;

import jakarta.validation.constraints.Size;

/**
 * Only needed when the invitation names no person: the name of the one that will be created.
 *
 * @param firstName First names of the person to create.
 * @param lastName Last names of the person to create.
 */
public record AcceptInvitationRequest(
    @Size(max = 100) String firstName, @Size(max = 100) String lastName) {}
