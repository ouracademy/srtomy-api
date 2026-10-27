package org.ouracademy.ovejafiel.notification.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.notification.command.send_email.EmailContent;
import org.ouracademy.ovejafiel.outbox.OutboxMessageHandler;
import org.ouracademy.ovejafiel.outbox.PermanentDeliveryException;
import org.ouracademy.ovejafiel.outbox.domain.OutboxChannel;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;
import org.springframework.stereotype.Component;

/** Sends the queued emails through the configured provider. */
@Component
@RequiredArgsConstructor
class EmailOutboxHandler implements OutboxMessageHandler {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final EmailGateway emailGateway;

  @Override
  public OutboxChannel channel() {
    return OutboxChannel.EMAIL;
  }

  @Override
  public String deliver(OutboxMessageId id, String recipient, String payload) {
    EmailContent content;
    try {
      content = JSON.readValue(payload, EmailContent.class);
    } catch (JsonProcessingException | IllegalArgumentException e) {
      // Not worth repeating: the same payload will not read the next time either.
      throw new PermanentDeliveryException("Unreadable email content", e);
    }
    return emailGateway.send(id.value().toString(), recipient, content.subject(), content.text());
  }
}
