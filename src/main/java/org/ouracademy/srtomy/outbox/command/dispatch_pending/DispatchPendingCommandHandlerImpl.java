package org.ouracademy.srtomy.outbox.command.dispatch_pending;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.srtomy.outbox.gateway.TaskGateway;
import org.ouracademy.srtomy.outbox.repository.OutboxMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
class DispatchPendingCommandHandlerImpl implements DispatchPendingCommandHandler {

  private final OutboxMessageRepository outboxMessageRepository;
  private final TaskGateway taskGateway;

  @Override
  @Transactional(readOnly = true)
  public DispatchPendingResult handle(DispatchPendingCommand command) {
    var pending =
        outboxMessageRepository.findPendingUntouchedSince(
            LocalDateTime.now().minus(command.untouchedFor()), PageRequest.of(0, command.limit()));
    int scheduled = 0;
    int couldNot = 0;
    for (var message : pending) {
      try {
        taskGateway.schedule(message.getId(), message.getChannel());
        scheduled++;
      } catch (RuntimeException e) {
        couldNot++;
        log.warn("Could not schedule outbox message {}: {}", message.getId(), e.toString());
      }
    }
    return new DispatchPendingResult(scheduled, couldNot);
  }
}
