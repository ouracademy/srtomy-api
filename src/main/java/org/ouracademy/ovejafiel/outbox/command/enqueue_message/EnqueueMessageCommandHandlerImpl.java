package org.ouracademy.ovejafiel.outbox.command.enqueue_message;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessage;
import org.ouracademy.ovejafiel.outbox.gateway.TaskGateway;
import org.ouracademy.ovejafiel.outbox.repository.OutboxMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class EnqueueMessageCommandHandlerImpl implements EnqueueMessageCommandHandler {

  private final OutboxMessageRepository outboxMessageRepository;
  private final TaskGateway taskGateway;

  /** MANDATORY: enqueuing outside a transaction would defeat the point of the outbox. */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public EnqueueMessageResult handle(EnqueueMessageCommand command) {
    var message =
        outboxMessageRepository.save(
            new OutboxMessage(
                command.channel(), command.type(), command.recipient(), command.payload()));
    if (command.dispatchNow()) {
      // Before the commit, on purpose: if the task cannot be created nothing is saved and the user
      // is told. If the commit fails afterwards, the task finds no message and does nothing.
      taskGateway.schedule(message.getId(), message.getChannel());
    }
    return new EnqueueMessageResult(message.getId());
  }
}
