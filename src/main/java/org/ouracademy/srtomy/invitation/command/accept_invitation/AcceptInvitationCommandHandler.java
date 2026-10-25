package org.ouracademy.srtomy.invitation.command.accept_invitation;

/**
 * Links the user to a person of the church and gives that person the invited position. Fails with:
 *
 * <ul>
 *   <li>{@code NoSuchElementException} (404): the invitation does not exist;
 *   <li>{@code AccessDeniedException} (403): the user's verified email is not the invited one;
 *   <li>{@code DomainValidationException} (400): the invitation is used, revoked or expired, the
 *       user already has another person in the church, or whoever invited can no longer give that
 *       access.
 * </ul>
 */
public interface AcceptInvitationCommandHandler {
  AcceptInvitationResult handle(AcceptInvitationCommand command);
}
