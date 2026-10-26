package org.ouracademy.srtomy.invitation.query.list_invitations_of_organization;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.invitation.domain.Invitation;
import org.ouracademy.srtomy.invitation.domain.InvitationStatus;
import org.ouracademy.srtomy.invitation.query.InvitationResult;
import org.ouracademy.srtomy.invitation.repository.InvitationRepository;
import org.ouracademy.srtomy.outbox.query.find_outbox_statuses.FindOutboxStatusesQuery;
import org.ouracademy.srtomy.outbox.query.find_outbox_statuses.FindOutboxStatusesQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ListInvitationsOfOrganizationQueryHandlerImpl
    implements ListInvitationsOfOrganizationQueryHandler {

  private final InvitationRepository invitationRepository;
  private final FindOutboxStatusesQueryHandler findOutboxStatusesQueryHandler;

  @Override
  @Transactional(readOnly = true)
  public List<InvitationResult> handle(ListInvitationsOfOrganizationQuery query) {
    var now = LocalDateTime.now();
    var pending =
        invitationRepository.findByOrganizationIdAndStatusOrderByCreateDateDesc(
            query.organizationId(), InvitationStatus.PENDING);
    var statuses =
        findOutboxStatusesQueryHandler.handle(
            new FindOutboxStatusesQuery(
                pending.stream().map(Invitation::getEmailMessageId).toList()));
    return pending.stream()
        .map(i -> InvitationResult.from(i, now).withEmailStatus(statuses.of(i.getEmailMessageId())))
        .toList();
  }
}
