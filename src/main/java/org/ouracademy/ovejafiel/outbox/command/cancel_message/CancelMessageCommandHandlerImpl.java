package org.ouracademy.ovejafiel.outbox.command.cancel_message;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.outbox.repository.OutboxMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CancelMessageCommandHandlerImpl implements CancelMessageCommandHandler {

  private final OutboxMessageRepository outboxMessageRepository;

  /**
   * The row is locked, so an attempt that is sending it right now finishes first: a message that
   * already left cannot be withdrawn, and this finds it SENT.
   */
  @Override
  @Transactional
  public CancelMessageResult handle(CancelMessageCommand command) {
    return outboxMessageRepository
        .findForUpdate(command.id())
        .map(message -> new CancelMessageResult(message.cancel()))
        .orElse(new CancelMessageResult(false));
  }
}
