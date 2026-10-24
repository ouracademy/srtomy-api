package org.ouracademy.srtomy.invitation.query.find_invitation_by_token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.invitation.InvitationFixtures;
import org.ouracademy.srtomy.invitation.domain.Invitation;
import org.ouracademy.srtomy.invitation.domain.InvitationStatus;
import org.ouracademy.srtomy.invitation.domain.InvitationTokens;
import org.ouracademy.srtomy.invitation.query.InvitationResult;
import org.ouracademy.srtomy.invitation.repository.InvitationRepository;
import org.ouracademy.srtomy.party.domain.PartyId;

class FindInvitationByTokenQueryHandlerImplTests {

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

  FindInvitationByTokenQueryHandlerImpl handler =
      new FindInvitationByTokenQueryHandlerImpl(repository);

  @Test
  void findsTheInvitationByTheHashOfItsToken() {
    var invitation = invitation(LocalDateTime.now().plusDays(1));
    when(repository.findByTokenHash(InvitationTokens.hash("token")))
        .thenReturn(Optional.of(invitation));

    assertThat(handler.handle(new FindInvitationByTokenQuery("token")))
        .map(InvitationResult::email)
        .contains("beto@example.org");
    assertThat(handler.handle(new FindInvitationByTokenQuery("other"))).isEmpty();
  }

  @Test
  void aBlankTokenFindsNothing() {
    assertThat(handler.handle(new FindInvitationByTokenQuery(" "))).isEmpty();
    assertThat(handler.handle(new FindInvitationByTokenQuery(null))).isEmpty();
  }

  @Test
  void anExpiredInvitationIsStillFoundButFlagged() {
    var expired = invitation(LocalDateTime.now().minusDays(1));
    when(repository.findByTokenHash(InvitationTokens.hash("token")))
        .thenReturn(Optional.of(expired));

    assertThat(handler.handle(new FindInvitationByTokenQuery("token")))
        .map(InvitationResult::status)
        .contains(InvitationStatus.EXPIRED);
  }
}
