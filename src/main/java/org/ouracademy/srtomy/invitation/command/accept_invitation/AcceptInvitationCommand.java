package org.ouracademy.srtomy.invitation.command.accept_invitation;

import org.ouracademy.srtomy.invitation.domain.InvitationId;

/**
 * {@code subject} is the signed-in user who accepts; their verified email must be the invited.
 * {@code firstName} and {@code lastName} name the person that is created when the invitation names
 * none (they are ignored when it does).
 */
public record AcceptInvitationCommand(
    String subject, InvitationId invitationId, String firstName, String lastName) {

  public AcceptInvitationCommand(String subject, InvitationId invitationId) {
    this(subject, invitationId, null, null);
  }
}
