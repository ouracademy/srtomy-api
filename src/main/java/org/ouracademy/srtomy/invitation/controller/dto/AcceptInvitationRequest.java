package org.ouracademy.srtomy.invitation.controller.dto;

import jakarta.validation.constraints.Size;

/** Only needed when the invitation names no person: the name of the one that will be created. */
public record AcceptInvitationRequest(
    @Size(max = 100) String firstName, @Size(max = 100) String lastName) {}
