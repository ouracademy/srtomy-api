package org.ouracademy.srtomy.notification.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.notification.command.send_email.EmailPayload;
import org.ouracademy.srtomy.outbox.OutboxMessageHandler;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;
import org.springframework.stereotype.Component;

/** Sends the queued emails through the configured provider. */
@Component
@RequiredArgsConstructor
class EmailOutboxHandler implements OutboxMessageHandler {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final EmailGateway emailGateway;

  @Override
  public String type() {
    return EmailPayload.TYPE;
  }

  @Override
  public void handle(OutboxMessageId id, String payload) {
    EmailPayload email;
    try {
      email = JSON.readValue(payload, EmailPayload.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unreadable email payload", e);
    }
    emailGateway.send(id.value().toString(), email.to(), email.subject(), email.text());
  }
}
