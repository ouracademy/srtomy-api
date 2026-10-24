package org.ouracademy.srtomy.invitation.command.create_invitation;

/**
 * Creates the invitation and emails the link. Inviting again to the same email and node replaces
 * the pending invitation, so it also works as "resend". Fails with {@code AccessDeniedException}
 * (403) when the caller lacks {@code invitations.send} or does not hold every permission of the
 * position, and with {@code DomainValidationException} (400) for a position of another church, a
 * person who is not in the node or already has an account, or a missing name.
 */
public interface CreateInvitationCommandHandler {
  CreateInvitationResult handle(CreateInvitationCommand command);
}
