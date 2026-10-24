package org.ouracademy.srtomy.invitation.query.list_invitations_for_email;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.invitation.domain.InvitationStatus;
import org.ouracademy.srtomy.invitation.query.InvitationResult;
import org.ouracademy.srtomy.invitation.repository.InvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ListInvitationsForEmailQueryHandlerImpl implements ListInvitationsForEmailQueryHandler {

  private final InvitationRepository invitationRepository;

  @Override
  @Transactional(readOnly = true)
  public List<InvitationResult> handle(ListInvitationsForEmailQuery query) {
    if (query.email() == null || query.email().isBlank()) {
      return List.of();
    }
    var now = LocalDateTime.now();
    return invitationRepository
        .findByEmailIgnoreCaseAndStatusOrderByCreateDateDesc(
            query.email().trim(), InvitationStatus.PENDING)
        .stream()
        .map(i -> InvitationResult.from(i, now))
        .filter(i -> i.status() == InvitationStatus.PENDING)
        .toList();
  }
}
