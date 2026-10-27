package org.ouracademy.ovejafiel.invitation.query.list_invitations_of_organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.invitation.InvitationFixtures;
import org.ouracademy.ovejafiel.invitation.domain.Invitation;
import org.ouracademy.ovejafiel.invitation.domain.InvitationStatus;
import org.ouracademy.ovejafiel.invitation.query.InvitationResult;
import org.ouracademy.ovejafiel.invitation.repository.InvitationRepository;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;
import org.ouracademy.ovejafiel.outbox.domain.OutboxStatus;
import org.ouracademy.ovejafiel.outbox.query.find_outbox_statuses.FindOutboxStatusesQueryHandler;
import org.ouracademy.ovejafiel.outbox.query.find_outbox_statuses.FindOutboxStatusesResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class ListInvitationsOfOrganizationQueryHandlerImplTests {

  InvitationRepository repository = mock(InvitationRepository.class);
  FindOutboxStatusesQueryHandler outboxStatuses = mock(FindOutboxStatusesQueryHandler.class);
  PartyId node = PartyId.generate();

  Invitation invitation(LocalDateTime expiresAt) {
    return InvitationFixtures.invitation(
        "beto@example.org",
        InvitationFixtures.organization(node, "Sede"),
        null,
        null,
        InvitationFixtures.user("auth0|a", "pastor@example.org"),
        expiresAt);
  }

  ListInvitationsOfOrganizationQueryHandlerImpl handler =
      new ListInvitationsOfOrganizationQueryHandlerImpl(repository, outboxStatuses);

  @BeforeEach
  void noStatuses() {
    when(outboxStatuses.handle(any())).thenReturn(new FindOutboxStatusesResult(Map.of()));
  }

  @Test
  void eachInvitationShowsTheStatusOfItsEmail() {
    var queued = invitation(LocalDateTime.now().plusDays(1));
    var messageId = OutboxMessageId.generate();
    queued.emailQueued(messageId);
    var old = invitation(LocalDateTime.now().plusDays(1));
    when(repository.findByOrganizationIdAndStatusOrderByCreateDateDesc(
            node, InvitationStatus.PENDING))
        .thenReturn(List.of(queued, old));
    when(outboxStatuses.handle(any()))
        .thenReturn(new FindOutboxStatusesResult(Map.of(messageId, OutboxStatus.FAILED)));

    assertThat(handler.handle(new ListInvitationsOfOrganizationQuery(node)))
        .extracting(InvitationResult::emailStatus)
        .containsExactly(OutboxStatus.FAILED, null);
  }

  @Test
  void listsThePendingOnesIncludingTheExpiredFlagged() {
    var valid = invitation(LocalDateTime.now().plusDays(1));
    var expired = invitation(LocalDateTime.now().minusDays(1));
    when(repository.findByOrganizationIdAndStatusOrderByCreateDateDesc(
            node, InvitationStatus.PENDING))
        .thenReturn(List.of(valid, expired));

    assertThat(handler.handle(new ListInvitationsOfOrganizationQuery(node)))
        .extracting(InvitationResult::status)
        .containsExactly(InvitationStatus.PENDING, InvitationStatus.EXPIRED);
  }

  @Test
  void aNodeWithoutInvitationsGetsAnEmptyList() {
    assertThat(handler.handle(new ListInvitationsOfOrganizationQuery(PartyId.generate())))
        .isEmpty();
  }
}
