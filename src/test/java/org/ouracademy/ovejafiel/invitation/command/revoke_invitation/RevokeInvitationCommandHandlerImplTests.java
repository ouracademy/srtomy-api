package org.ouracademy.ovejafiel.invitation.command.revoke_invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.invitation.domain.Invitation;
import org.ouracademy.ovejafiel.invitation.domain.InvitationId;
import org.ouracademy.ovejafiel.invitation.repository.InvitationRepository;

class RevokeInvitationCommandHandlerImplTests {

  InvitationRepository repository = mock(InvitationRepository.class);
  RevokeInvitationCommandHandlerImpl handler = new RevokeInvitationCommandHandlerImpl(repository);

  @Test
  void revokesThePendingInvitation() {
    var id = InvitationId.generate();
    var invitation = mock(Invitation.class);
    when(repository.findById(id)).thenReturn(Optional.of(invitation));

    assertThat(handler.handle(new RevokeInvitationCommand(id))).isNotNull();

    verify(invitation).revoke();
  }

  @Test
  void anUnknownInvitationIsNotFound() {
    var id = InvitationId.generate();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new RevokeInvitationCommand(id)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
