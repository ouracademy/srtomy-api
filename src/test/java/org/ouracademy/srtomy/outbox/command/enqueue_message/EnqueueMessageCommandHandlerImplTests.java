package org.ouracademy.srtomy.outbox.command.enqueue_message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.srtomy.outbox.OutboxMessageEnqueued;
import org.ouracademy.srtomy.outbox.domain.OutboxMessage;
import org.ouracademy.srtomy.outbox.domain.OutboxStatus;
import org.ouracademy.srtomy.outbox.repository.OutboxMessageRepository;
import org.springframework.context.ApplicationEventPublisher;

class EnqueueMessageCommandHandlerImplTests {

  OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
  ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
  EnqueueMessageCommandHandlerImpl handler =
      new EnqueueMessageCommandHandlerImpl(repository, events);

  @Test
  void savesAPendingMessageAndAnnouncesIt() {
    when(repository.save(any(OutboxMessage.class))).thenAnswer(call -> call.getArgument(0));

    var result = handler.handle(new EnqueueMessageCommand("EMAIL", "{\"to\":\"a@x.org\"}"));

    var saved = ArgumentCaptor.forClass(OutboxMessage.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().getType()).isEqualTo("EMAIL");
    assertThat(saved.getValue().getPayload()).isEqualTo("{\"to\":\"a@x.org\"}");
    assertThat(saved.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(result.id()).isEqualTo(saved.getValue().getId());
    verify(events).publishEvent(new OutboxMessageEnqueued());
  }
}
