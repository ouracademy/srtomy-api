package org.ouracademy.ovejafiel.invitation.command.revoke_invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.invitation.domain.Invitation;
import org.ouracademy.ovejafiel.invitation.domain.InvitationId;
import org.ouracademy.ovejafiel.invitation.repository.InvitationRepository;
import org.ouracademy.ovejafiel.outbox.command.cancel_message.CancelMessageCommand;
import org.ouracademy.ovejafiel.outbox.command.cancel_message.CancelMessageCommandHandler;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;

class RevokeInvitationCommandHandlerImplTests {

  InvitationRepository repository = mock(InvitationRepository.class);
  CancelMessageCommandHandler cancelMessage = mock(CancelMessageCommandHandler.class);
  RevokeInvitationCommandHandlerImpl handler =
      new RevokeInvitationCommandHandlerImpl(repository, cancelMessage);

  @Test
  void revokesThePendingInvitation() {
    var id = InvitationId.generate();
    var invitation = mock(Invitation.class);
    when(repository.findById(id)).thenReturn(Optional.of(invitation));

    assertThat(handler.handle(new RevokeInvitationCommand(id))).isNotNull();

    verify(invitation).revoke();
  }

  @Test
  void theEmailThatHasNotLeftYetIsWithdrawn() {
    var id = InvitationId.generate();
    var messageId = OutboxMessageId.generate();
    var invitation = mock(Invitation.class);
    when(invitation.getEmailMessageId()).thenReturn(messageId);
    when(repository.findById(id)).thenReturn(Optional.of(invitation));

    handler.handle(new RevokeInvitationCommand(id));

    verify(cancelMessage).handle(new CancelMessageCommand(messageId));
  }

  @Test
  void anInvitationFromBeforeTheOutboxHasNoEmailToWithdraw() {
    var id = InvitationId.generate();
    var invitation = mock(Invitation.class);
    when(repository.findById(id)).thenReturn(Optional.of(invitation));

    handler.handle(new RevokeInvitationCommand(id));

    verifyNoInteractions(cancelMessage);
  }

  @Test
  void anUnknownInvitationIsNotFound() {
    var id = InvitationId.generate();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new RevokeInvitationCommand(id)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
