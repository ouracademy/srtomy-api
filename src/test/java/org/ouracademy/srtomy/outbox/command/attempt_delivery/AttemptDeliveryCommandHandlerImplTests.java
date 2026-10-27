package org.ouracademy.srtomy.outbox.command.attempt_delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.outbox.OutboxMessageHandler;
import org.ouracademy.srtomy.outbox.PermanentDeliveryException;
import org.ouracademy.srtomy.outbox.command.attempt_delivery.AttemptDeliveryResult.Outcome;
import org.ouracademy.srtomy.outbox.domain.OutboxChannel;
import org.ouracademy.srtomy.outbox.domain.OutboxMessage;
import org.ouracademy.srtomy.outbox.domain.OutboxStatus;
import org.ouracademy.srtomy.outbox.repository.OutboxMessageRepository;

class AttemptDeliveryCommandHandlerImplTests {

  OutboxMessageRepository repository = mock(OutboxMessageRepository.class);
  OutboxMessageHandler email = mock(OutboxMessageHandler.class);
  AttemptDeliveryCommandHandlerImpl handler;
  OutboxMessage message =
      new OutboxMessage(OutboxChannel.EMAIL, "INVITATION", "ana@x.org", "{\"text\":\"hola\"}");

  AttemptDeliveryCommandHandlerImplTests() {
    when(email.channel()).thenReturn(OutboxChannel.EMAIL);
    handler = new AttemptDeliveryCommandHandlerImpl(repository, List.of(email), 3);
    when(repository.findForUpdate(message.getId())).thenReturn(Optional.of(message));
  }

  AttemptDeliveryResult attempt() {
    return handler.handle(new AttemptDeliveryCommand(message.getId()));
  }

  @Test
  void deliversTheMessageAndMarksItSent() {
    when(email.deliver(message.getId(), "ana@x.org", "{\"text\":\"hola\"}")).thenReturn("re_1");

    assertThat(attempt().outcome()).isEqualTo(Outcome.SENT);

    assertThat(message.getStatus()).isEqualTo(OutboxStatus.SENT);
    assertThat(message.getProviderMessageId()).isEqualTo("re_1");
    assertThat(message.getPayload()).isNull();
  }

  @Test
  void aMessageThatIsNotThereMeansTheEnqueuingTransactionDidNotCommit() {
    var unknown = new OutboxMessage(OutboxChannel.EMAIL, "X", "b@x.org", "{}");

    var result = handler.handle(new AttemptDeliveryCommand(unknown.getId()));

    assertThat(result.outcome()).isEqualTo(Outcome.NOT_FOUND);
    assertThat(result.shouldRetry()).isFalse();
  }

  @Test
  void aMessageAlreadyDoneIsNotDeliveredAgain() {
    message.markSent("re_1", java.time.LocalDateTime.now());

    assertThat(attempt().outcome()).isEqualTo(Outcome.ALREADY_DONE);

    verify(email, never()).deliver(any(), any(), any());
  }

  @Test
  void aTransientFailureAsksTheQueueToCallAgain() {
    when(email.deliver(any(), any(), any())).thenThrow(new IllegalStateException("503"));

    var result = attempt();

    assertThat(result.outcome()).isEqualTo(Outcome.RETRY);
    assertThat(result.shouldRetry()).isTrue();
    assertThat(message.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(message.getAttempts()).isEqualTo(1);
    assertThat(message.getLastError()).contains("503");
    assertThat(message.getPayload()).isNotNull();
  }

  @Test
  void givesUpWhenTheAttemptsRunOut() {
    when(email.deliver(any(), any(), any())).thenThrow(new IllegalStateException("503"));

    assertThat(attempt().outcome()).isEqualTo(Outcome.RETRY);
    assertThat(attempt().outcome()).isEqualTo(Outcome.RETRY);
    var last = attempt();

    assertThat(last.outcome()).isEqualTo(Outcome.FAILED);
    assertThat(last.shouldRetry()).isFalse();
    assertThat(message.getStatus()).isEqualTo(OutboxStatus.FAILED);
    assertThat(message.getAttempts()).isEqualTo(3);
    assertThat(message.getPayload()).isNull();
  }

  @Test
  void aPermanentFailureGivesUpAtOnce() {
    when(email.deliver(any(), any(), any()))
        .thenThrow(new PermanentDeliveryException("403 domain not verified", null));

    var result = attempt();

    assertThat(result.outcome()).isEqualTo(Outcome.FAILED);
    assertThat(message.getStatus()).isEqualTo(OutboxStatus.FAILED);
    assertThat(message.getLastError()).isEqualTo("403 domain not verified");
  }

  @Test
  void aChannelWithoutHandlerGivesUp() {
    var whatsapp = new OutboxMessage(OutboxChannel.WHATSAPP, "BIRTHDAY", "+51999", "{}");
    when(repository.findForUpdate(whatsapp.getId())).thenReturn(Optional.of(whatsapp));

    var result = handler.handle(new AttemptDeliveryCommand(whatsapp.getId()));

    assertThat(result.outcome()).isEqualTo(Outcome.FAILED);
    assertThat(whatsapp.getStatus()).isEqualTo(OutboxStatus.FAILED);
  }
}
