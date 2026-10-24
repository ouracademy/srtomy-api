package org.ouracademy.srtomy.invitation.query.find_invitation_by_id;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.invitation.query.InvitationResult;
import org.ouracademy.srtomy.invitation.repository.InvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindInvitationByIdQueryHandlerImpl implements FindInvitationByIdQueryHandler {

  private final InvitationRepository invitationRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<InvitationResult> handle(FindInvitationByIdQuery query) {
    var now = LocalDateTime.now();
    return invitationRepository.findById(query.id()).map(i -> InvitationResult.from(i, now));
  }
}
