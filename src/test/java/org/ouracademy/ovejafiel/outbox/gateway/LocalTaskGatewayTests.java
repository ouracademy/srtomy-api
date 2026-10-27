package org.ouracademy.ovejafiel.outbox.gateway;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.outbox.command.attempt_delivery.AttemptDeliveryCommand;
import org.ouracademy.ovejafiel.outbox.command.attempt_delivery.AttemptDeliveryCommandHandler;
import org.ouracademy.ovejafiel.outbox.domain.OutboxChannel;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class LocalTaskGatewayTests {

  AttemptDeliveryCommandHandler attempt = mock(AttemptDeliveryCommandHandler.class);
  LocalTaskGateway gateway = new LocalTaskGateway(attempt);
  OutboxMessageId id = OutboxMessageId.generate();

  @Test
  void outsideATransactionItAttemptsAtOnce() {
    gateway.schedule(id, OutboxChannel.EMAIL);

    verify(attempt).handle(new AttemptDeliveryCommand(id));
  }

  @Test
  void insideATransactionItWaitsForTheCommit() {
    TransactionSynchronizationManager.initSynchronization();
    try {
      gateway.schedule(id, OutboxChannel.EMAIL);
      verify(attempt, never()).handle(any());

      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);

      verify(attempt).handle(new AttemptDeliveryCommand(id));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void aFailureIsLoggedNotThrown() {
    when(attempt.handle(any())).thenThrow(new IllegalStateException("db down"));

    assertThatCode(() -> gateway.schedule(id, OutboxChannel.EMAIL)).doesNotThrowAnyException();
  }
}
