package org.ouracademy.ovejafiel.invitation.command.revoke_invitation;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.invitation.repository.InvitationRepository;
import org.ouracademy.ovejafiel.outbox.command.cancel_message.CancelMessageCommand;
import org.ouracademy.ovejafiel.outbox.command.cancel_message.CancelMessageCommandHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class RevokeInvitationCommandHandlerImpl implements RevokeInvitationCommandHandler {

  private final InvitationRepository invitationRepository;
  private final CancelMessageCommandHandler cancelMessageCommandHandler;

  @Override
  @Transactional
  public RevokeInvitationResult handle(RevokeInvitationCommand command) {
    var invitation =
        invitationRepository
            .findById(command.invitationId())
            .orElseThrow(() -> new NoSuchElementException("Invitation not found"));
    invitation.revoke();
    // The email may not have left yet: it must not go out with a link that no longer works.
    if (invitation.getEmailMessageId() != null) {
      cancelMessageCommandHandler.handle(new CancelMessageCommand(invitation.getEmailMessageId()));
    }
    return new RevokeInvitationResult();
  }
}
