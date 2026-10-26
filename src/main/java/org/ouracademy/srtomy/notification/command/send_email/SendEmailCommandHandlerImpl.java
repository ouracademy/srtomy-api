package org.ouracademy.srtomy.notification.command.send_email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.outbox.command.enqueue_message.EnqueueMessageCommand;
import org.ouracademy.srtomy.outbox.command.enqueue_message.EnqueueMessageCommandHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Queues the email in the caller's transaction (see the outbox): it is sent right after the commit
 * and retried if the provider fails, so it exists if and only if the caller's change does.
 */
@Service
@RequiredArgsConstructor
class SendEmailCommandHandlerImpl implements SendEmailCommandHandler {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final EnqueueMessageCommandHandler enqueueMessageCommandHandler;

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public SendEmailResult handle(SendEmailCommand command) {
    var payload = new EmailPayload(command.to(), command.subject(), command.text());
    try {
      var queued =
          enqueueMessageCommandHandler.handle(
              new EnqueueMessageCommand(EmailPayload.TYPE, JSON.writeValueAsString(payload)));
      return new SendEmailResult(queued.id());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Could not write the email payload", e);
    }
  }
}
