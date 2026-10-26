package org.ouracademy.ovejafiel.invitation.command.revoke_invitation;

/** Cancels a pending invitation so its link stops working. Revoking a used one changes nothing. */
public interface RevokeInvitationCommandHandler {
  /**
   * @throws java.util.NoSuchElementException when the invitation does not exist
   */
  RevokeInvitationResult handle(RevokeInvitationCommand command);
}
