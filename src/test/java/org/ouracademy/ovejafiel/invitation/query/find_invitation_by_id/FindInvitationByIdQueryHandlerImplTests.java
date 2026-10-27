package org.ouracademy.ovejafiel.invitation.query.find_invitation_by_id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.invitation.InvitationFixtures;
import org.ouracademy.ovejafiel.invitation.domain.Invitation;
import org.ouracademy.ovejafiel.invitation.domain.InvitationId;
import org.ouracademy.ovejafiel.invitation.domain.InvitationStatus;
import org.ouracademy.ovejafiel.invitation.domain.InvitationTokens;
import org.ouracademy.ovejafiel.invitation.repository.InvitationRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class FindInvitationByIdQueryHandlerImplTests {

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

  FindInvitationByIdQueryHandlerImpl handler = new FindInvitationByIdQueryHandlerImpl(repository);

  @Test
  void findsTheInvitationWithItsNamesButNeverTheToken() {
    var id = InvitationId.generate();
    var invitation = invitation(LocalDateTime.now().plusDays(1));
    when(repository.findById(id)).thenReturn(Optional.of(invitation));

    var result = handler.handle(new FindInvitationByIdQuery(id)).orElseThrow();

    assertThat(result.organizationName()).isEqualTo("Sede");
    assertThat(result.personName()).isEqualTo("Beto Pérez");
    assertThat(result.invitedByEmail()).isEqualTo("pastor@example.org");
    assertThat(result.positionId()).isNull();
    assertThat(result.status()).isEqualTo(InvitationStatus.PENDING);
    assertThat(result.toString()).doesNotContain(InvitationTokens.hash("token"));
  }

  @Test
  void anUnknownIdIsEmpty() {
    assertThat(handler.handle(new FindInvitationByIdQuery(InvitationId.generate()))).isEmpty();
  }
}
