package org.ouracademy.srtomy.invitation.query.list_invitations_of_organization;

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
class ListInvitationsOfOrganizationQueryHandlerImpl
    implements ListInvitationsOfOrganizationQueryHandler {

  private final InvitationRepository invitationRepository;

  @Override
  @Transactional(readOnly = true)
  public List<InvitationResult> handle(ListInvitationsOfOrganizationQuery query) {
    var now = LocalDateTime.now();
    return invitationRepository
        .findByOrganizationIdAndStatusOrderByCreateDateDesc(
            query.organizationId(), InvitationStatus.PENDING)
        .stream()
        .map(i -> InvitationResult.from(i, now))
        .toList();
  }
}
