package org.ouracademy.srtomy.invitation.query.list_invitations_of_organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.invitation.InvitationFixtures;
import org.ouracademy.srtomy.invitation.domain.Invitation;
import org.ouracademy.srtomy.invitation.domain.InvitationStatus;
import org.ouracademy.srtomy.invitation.query.InvitationResult;
import org.ouracademy.srtomy.invitation.repository.InvitationRepository;
import org.ouracademy.srtomy.party.domain.PartyId;

class ListInvitationsOfOrganizationQueryHandlerImplTests {

  InvitationRepository repository = mock(InvitationRepository.class);
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
      new ListInvitationsOfOrganizationQueryHandlerImpl(repository);

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
