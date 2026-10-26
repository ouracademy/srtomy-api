package org.ouracademy.ovejafiel.invitation.query.find_invitation_by_token;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.invitation.domain.InvitationTokens;
import org.ouracademy.ovejafiel.invitation.query.InvitationResult;
import org.ouracademy.ovejafiel.invitation.repository.InvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindInvitationByTokenQueryHandlerImpl implements FindInvitationByTokenQueryHandler {

  private final InvitationRepository invitationRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<InvitationResult> handle(FindInvitationByTokenQuery query) {
    if (query.token() == null || query.token().isBlank()) {
      return Optional.empty();
    }
    var now = LocalDateTime.now();
    return invitationRepository
        .findByTokenHash(InvitationTokens.hash(query.token()))
        .map(i -> InvitationResult.from(i, now));
  }
}
