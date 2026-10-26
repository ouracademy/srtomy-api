package org.ouracademy.srtomy.outbox.command.drain_outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.srtomy.outbox.OutboxMessageHandler;
import org.ouracademy.srtomy.outbox.domain.OutboxStatus;
import org.ouracademy.srtomy.outbox.repository.OutboxMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Attempts the due messages one by one, each in its own transaction that holds the row lock while
 * the handler runs: if the process dies mid-way the transaction rolls back and the message is due
 * again, and two instances never take the same message.
 */
@Slf4j
@Service
class DrainOutboxCommandHandlerImpl implements DrainOutboxCommandHandler {

  private final OutboxMessageRepository outboxMessageRepository;
  private final TransactionTemplate transactionTemplate;
  private final Map<String, OutboxMessageHandler> handlers;

  DrainOutboxCommandHandlerImpl(
      OutboxMessageRepository outboxMessageRepository,
      PlatformTransactionManager transactionManager,
      List<OutboxMessageHandler> handlers) {
    this.outboxMessageRepository = outboxMessageRepository;
    // REQUIRES_NEW: right after a commit the caller's transaction is still bound to the thread.
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.handlers =
        handlers.stream()
            .collect(Collectors.toMap(OutboxMessageHandler::type, Function.identity()));
  }

  @Override
  public DrainOutboxResult handle(DrainOutboxCommand command) {
    int sent = 0;
    int retrying = 0;
    int failed = 0;
    for (int i = 0; i < command.limit(); i++) {
      var outcome = transactionTemplate.execute(status -> attemptNext());
      if (outcome == null) {
        break;
      }
      switch (outcome) {
        case SENT -> sent++;
        case PENDING -> retrying++;
        case FAILED -> failed++;
      }
    }
    return new DrainOutboxResult(sent, retrying, failed);
  }

  /** Null when nothing is due. */
  private OutboxStatus attemptNext() {
    var now = LocalDateTime.now();
    var message = outboxMessageRepository.lockNextDue(now).orElse(null);
    if (message == null) {
      return null;
    }
    var handler = handlers.get(message.getType());
    if (handler == null) {
      message.giveUp("No handler for type " + message.getType());
      log.error("Outbox message {} has no handler for type {}", message.getId(), message.getType());
      return message.getStatus();
    }
    try {
      handler.handle(message.getId(), message.getPayload());
      message.markSent(LocalDateTime.now());
    } catch (RuntimeException e) {
      var status = message.markFailed(e.toString(), LocalDateTime.now());
      log.warn(
          "Outbox message {} ({}) failed, attempt {}: {}",
          message.getId(),
          message.getType(),
          message.getAttempts(),
          e.toString());
      return status;
    }
    return message.getStatus();
  }
}
