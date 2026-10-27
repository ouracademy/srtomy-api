package org.ouracademy.srtomy.outbox.command.cancel_message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.outbox.domain.OutboxChannel;
import org.ouracademy.srtomy.outbox.domain.OutboxMessage;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;
import org.ouracademy.srtomy.outbox.domain.OutboxStatus;
import org.ouracademy.srtomy.outbox.repository.OutboxMessageRepository;

class CancelMessageCommandHandlerImplTests {

  OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
  CancelMessageCommandHandlerImpl handler = new CancelMessageCommandHandlerImpl(repository);
  OutboxMessage message = new OutboxMessage(OutboxChannel.EMAIL, "INVITATION", "a@x.org", "{}");

  @Test
  void cancelsAMessageThatStillWaits() {
    when(repository.findForUpdate(message.getId())).thenReturn(Optional.of(message));

    assertThat(handler.handle(new CancelMessageCommand(message.getId())).cancelled()).isTrue();

    assertThat(message.getStatus()).isEqualTo(OutboxStatus.CANCELLED);
  }

  @Test
  void aMessageThatAlreadyLeftIsLeftAlone() {
    message.markSent("re_1", LocalDateTime.now());
    when(repository.findForUpdate(message.getId())).thenReturn(Optional.of(message));

    assertThat(handler.handle(new CancelMessageCommand(message.getId())).cancelled()).isFalse();

    assertThat(message.getStatus()).isEqualTo(OutboxStatus.SENT);
  }

  @Test
  void anUnknownMessageIsNotAnError() {
    var unknown = OutboxMessageId.generate();
    when(repository.findForUpdate(unknown)).thenReturn(Optional.empty());

    assertThat(handler.handle(new CancelMessageCommand(unknown)).cancelled()).isFalse();
  }
}
