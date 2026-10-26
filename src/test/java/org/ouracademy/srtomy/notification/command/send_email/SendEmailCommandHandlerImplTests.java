package org.ouracademy.srtomy.notification.command.send_email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.outbox.command.enqueue_message.EnqueueMessageCommand;
import org.ouracademy.srtomy.outbox.command.enqueue_message.EnqueueMessageCommandHandler;
import org.ouracademy.srtomy.outbox.command.enqueue_message.EnqueueMessageResult;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;

@ExtendWith(MockitoExtension.class)
class SendEmailCommandHandlerImplTests {

  @Mock EnqueueMessageCommandHandler enqueue;
  @InjectMocks SendEmailCommandHandlerImpl handler;

  @Test
  void queuesTheEmailInTheOutboxAndReturnsItsId() {
    var id = OutboxMessageId.generate();
    when(enqueue.handle(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new EnqueueMessageResult(id));

    var result =
        handler.handle(new SendEmailCommand("ana@x.org", "Hola", "Texto \"con\" comillas"));

    assertThat(result.messageId()).isEqualTo(id);
    var queued = ArgumentCaptor.forClass(EnqueueMessageCommand.class);
    verify(enqueue).handle(queued.capture());
    assertThat(queued.getValue().type()).isEqualTo("EMAIL");
    assertThat(queued.getValue().payload())
        .isEqualTo(
            "{\"to\":\"ana@x.org\",\"subject\":\"Hola\",\"text\":\"Texto \\\"con\\\" comillas\"}");
  }
}
