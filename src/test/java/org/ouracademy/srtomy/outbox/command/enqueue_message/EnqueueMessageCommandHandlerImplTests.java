package org.ouracademy.srtomy.outbox.command.enqueue_message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.srtomy.outbox.domain.OutboxChannel;
import org.ouracademy.srtomy.outbox.domain.OutboxMessage;
import org.ouracademy.srtomy.outbox.domain.OutboxStatus;
import org.ouracademy.srtomy.outbox.gateway.TaskGateway;
import org.ouracademy.srtomy.outbox.repository.OutboxMessageRepository;

class EnqueueMessageCommandHandlerImplTests {

  OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
  TaskGateway tasks = mock(TaskGateway.class);
  EnqueueMessageCommandHandlerImpl handler =
      new EnqueueMessageCommandHandlerImpl(repository, tasks);

  @BeforeEach
  void saving() {
    when(repository.save(any(OutboxMessage.class))).thenAnswer(call -> call.getArgument(0));
  }

  @Test
  void savesAPendingMessageAndSchedulesItsDelivery() {
    var result =
        handler.handle(
            new EnqueueMessageCommand(OutboxChannel.EMAIL, "INVITATION", "a@x.org", "{}"));

    var saved = ArgumentCaptor.forClass(OutboxMessage.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().getChannel()).isEqualTo(OutboxChannel.EMAIL);
    assertThat(saved.getValue().getType()).isEqualTo("INVITATION");
    assertThat(saved.getValue().getRecipient()).isEqualTo("a@x.org");
    assertThat(saved.getValue().getPayload()).isEqualTo("{}");
    assertThat(saved.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(result.id()).isEqualTo(saved.getValue().getId());
    verify(tasks).schedule(saved.getValue().getId(), OutboxChannel.EMAIL);
  }

  @Test
  void aBulkMessageIsSavedWithoutATaskForTheSweepToSchedule() {
    handler.handle(
        new EnqueueMessageCommand(OutboxChannel.WHATSAPP, "BIRTHDAY", "+51999", "{}", false));

    verify(repository).save(any(OutboxMessage.class));
    verifyNoInteractions(tasks);
  }

  @Test
  void ifTheTaskCannotBeCreatedTheCallerIsToldSoItsTransactionRollsBack() {
    doThrow(new IllegalStateException("cloud tasks down")).when(tasks).schedule(any(), any());

    assertThatThrownBy(
            () ->
                handler.handle(
                    new EnqueueMessageCommand(OutboxChannel.EMAIL, "INVITATION", "a@x.org", "{}")))
        .isInstanceOf(IllegalStateException.class);
  }
}
