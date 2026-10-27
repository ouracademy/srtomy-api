package org.ouracademy.ovejafiel.invitation.command.create_invitation;

import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Invites {@code email} to use the app in {@code organizationId}, as {@code personId} (an existing
 * person of the node) or, when that is null, as a new person called {@code firstName} {@code
 * lastName}. {@code positionId} (optional) is the position the person takes on accepting. {@code
 * subject} is the user who invites.
 */
public record CreateInvitationCommand(
    String subject,
    String email,
    PartyId organizationId,
    AccountabilityTypeId positionId,
    PartyId personId,
    String firstName,
    String lastName) {}
