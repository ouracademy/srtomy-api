package org.ouracademy.srtomy.notification.command.send_email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.outbox.command.enqueue_message.EnqueueMessageCommand;
import org.ouracademy.srtomy.outbox.command.enqueue_message.EnqueueMessageCommandHandler;
import org.ouracademy.srtomy.outbox.domain.OutboxChannel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Queues the email in the caller's transaction (see the outbox): a task sends it soon after the
 * commit and the queue retries it, so it exists if and only if the caller's change does.
 */
@Service
@RequiredArgsConstructor
class SendEmailCommandHandlerImpl implements SendEmailCommandHandler {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final EnqueueMessageCommandHandler enqueueMessageCommandHandler;

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public SendEmailResult handle(SendEmailCommand command) {
    var content = new EmailContent(command.subject(), command.text());
    try {
      var queued =
          enqueueMessageCommandHandler.handle(
              new EnqueueMessageCommand(
                  OutboxChannel.EMAIL,
                  command.type(),
                  command.to(),
                  JSON.writeValueAsString(content)));
      return new SendEmailResult(queued.id());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Could not write the email content", e);
    }
  }
}
