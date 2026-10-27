package org.ouracademy.srtomy.outbox.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.srtomy.outbox.command.attempt_delivery.AttemptDeliveryCommand;
import org.ouracademy.srtomy.outbox.command.attempt_delivery.AttemptDeliveryCommandHandler;
import org.ouracademy.srtomy.outbox.domain.OutboxChannel;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Used when no queue is configured (local runs and tests): the delivery is attempted once, right
 * after the transaction commits, and never retried.
 */
@Slf4j
@RequiredArgsConstructor
class LocalTaskGateway implements TaskGateway {

  private final AttemptDeliveryCommandHandler attemptDeliveryCommandHandler;

  @Override
  public void schedule(OutboxMessageId id, OutboxChannel channel) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              attempt(id);
            }
          });
    } else {
      attempt(id);
    }
  }

  private void attempt(OutboxMessageId id) {
    try {
      attemptDeliveryCommandHandler.handle(new AttemptDeliveryCommand(id));
    } catch (RuntimeException e) {
      log.warn("Local delivery attempt of {} failed", id, e);
    }
  }
}
