package org.ouracademy.srtomy.invitation.command.revoke_invitation;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.invitation.repository.InvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class RevokeInvitationCommandHandlerImpl implements RevokeInvitationCommandHandler {

  private final InvitationRepository invitationRepository;

  @Override
  @Transactional
  public RevokeInvitationResult handle(RevokeInvitationCommand command) {
    invitationRepository
        .findById(command.invitationId())
        .orElseThrow(() -> new NoSuchElementException("Invitation not found"))
        .revoke();
    return new RevokeInvitationResult();
  }
}
