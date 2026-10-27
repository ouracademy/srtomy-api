package org.ouracademy.srtomy.outbox.command.attempt_delivery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.srtomy.outbox.OutboxMessageHandler;
import org.ouracademy.srtomy.outbox.PermanentDeliveryException;
import org.ouracademy.srtomy.outbox.command.attempt_delivery.AttemptDeliveryResult.Outcome;
import org.ouracademy.srtomy.outbox.domain.OutboxChannel;
import org.ouracademy.srtomy.outbox.domain.OutboxStatus;
import org.ouracademy.srtomy.outbox.repository.OutboxMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * One delivery attempt, called by the queue's task. The retries are the queue's: this only says
 * whether to call again ({@code RETRY}) and records how each attempt went. The message row is
 * locked while the provider is called, so a repeated task waits and then finds it done.
 */
@Slf4j
@Service
class AttemptDeliveryCommandHandlerImpl implements AttemptDeliveryCommandHandler {

  private final OutboxMessageRepository outboxMessageRepository;
  private final Map<OutboxChannel, OutboxMessageHandler> handlers;
  private final int maxAttempts;

  AttemptDeliveryCommandHandlerImpl(
      OutboxMessageRepository outboxMessageRepository,
      List<OutboxMessageHandler> handlers,
      // The same number as max_attempts of the queues (infra/terraform/modules/tasks).
      @Value("${outbox.max-attempts:10}") int maxAttempts) {
    this.outboxMessageRepository = outboxMessageRepository;
    this.handlers =
        handlers.stream()
            .collect(Collectors.toMap(OutboxMessageHandler::channel, Function.identity()));
    this.maxAttempts = maxAttempts;
  }

  /**
   * REQUIRES_NEW: in local runs the attempt happens right after another transaction's commit, which
   * is still bound to the thread then.
   */
  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AttemptDeliveryResult handle(AttemptDeliveryCommand command) {
    var message = outboxMessageRepository.findForUpdate(command.id()).orElse(null);
    if (message == null) {
      return new AttemptDeliveryResult(Outcome.NOT_FOUND);
    }
    if (message.getStatus() != OutboxStatus.PENDING) {
      return new AttemptDeliveryResult(Outcome.ALREADY_DONE);
    }
    var handler = handlers.get(message.getChannel());
    if (handler == null) {
      message.markFailed("No handler for channel " + message.getChannel());
      log.error("Outbox message {} has no handler for {}", message.getId(), message.getChannel());
      return new AttemptDeliveryResult(Outcome.FAILED);
    }
    try {
      var providerId =
          handler.deliver(message.getId(), message.getRecipient(), message.getPayload());
      message.markSent(providerId, LocalDateTime.now());
      return new AttemptDeliveryResult(Outcome.SENT);
    } catch (PermanentDeliveryException e) {
      message.markFailed(e.getMessage());
      log.warn("Outbox message {} refused for good: {}", message.getId(), e.getMessage());
      return new AttemptDeliveryResult(Outcome.FAILED);
    } catch (RuntimeException e) {
      message.recordFailedAttempt(e.toString());
      if (message.getAttempts() >= maxAttempts) {
        message.markFailed(e.toString());
        log.error("Outbox message {} gave up after {} attempts", message.getId(), maxAttempts, e);
        return new AttemptDeliveryResult(Outcome.FAILED);
      }
      log.warn(
          "Outbox message {} failed, attempt {}: {}",
          message.getId(),
          message.getAttempts(),
          e.toString());
      return new AttemptDeliveryResult(Outcome.RETRY);
    }
  }
}
