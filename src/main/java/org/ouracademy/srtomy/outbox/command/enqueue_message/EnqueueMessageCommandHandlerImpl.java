package org.ouracademy.srtomy.outbox.command.enqueue_message;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.outbox.OutboxMessageEnqueued;
import org.ouracademy.srtomy.outbox.domain.OutboxMessage;
import org.ouracademy.srtomy.outbox.repository.OutboxMessageRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class EnqueueMessageCommandHandlerImpl implements EnqueueMessageCommandHandler {

  private final OutboxMessageRepository outboxMessageRepository;
  private final ApplicationEventPublisher events;

  /** MANDATORY: enqueuing outside a transaction would defeat the point of the outbox. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public EnqueueMessageResult handle(EnqueueMessageCommand command) {
    var message =
        outboxMessageRepository.save(
            new OutboxMessage(command.type(), command.payload(), LocalDateTime.now()));
    events.publishEvent(new OutboxMessageEnqueued());
    return new EnqueueMessageResult(message.getId());
  }
}
