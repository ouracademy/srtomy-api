package org.ouracademy.srtomy.outbox.command.drain_outbox;

import lombok.extern.slf4j.Slf4j;
import org.ouracademy.srtomy.outbox.OutboxMessageEnqueued;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Tries the new messages as soon as the transaction that saved them commits, so an email leaves in
 * a second instead of waiting for the next scheduler tick. Best effort: the scheduler's call to
 * {@code POST /internal/outbox/drain} is what guarantees delivery.
 */
@Slf4j
@Component
class DrainOnCommit {

  private static final int LIMIT = 10;

  private final DrainOutboxCommandHandler drainOutboxCommandHandler;

  DrainOnCommit(DrainOutboxCommandHandler drainOutboxCommandHandler) {
    this.drainOutboxCommandHandler = drainOutboxCommandHandler;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onEnqueued(OutboxMessageEnqueued event) {
    try {
      drainOutboxCommandHandler.handle(new DrainOutboxCommand(LIMIT));
    } catch (RuntimeException e) {
      log.warn("Could not drain the outbox right after commit; the scheduler will", e);
    }
  }
}
