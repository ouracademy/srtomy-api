package org.ouracademy.ovejafiel.whatsapp.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.outbox.OutboxMessageHandler;
import org.ouracademy.ovejafiel.outbox.PermanentDeliveryException;
import org.ouracademy.ovejafiel.outbox.domain.OutboxChannel;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppTemplate;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.springframework.stereotype.Component;

/** Sends the queued WhatsApp messages from the number the church connected. */
@Component
@RequiredArgsConstructor
class WhatsAppOutboxHandler implements OutboxMessageHandler {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final WhatsAppConnectionRepository connections;
  private final WhatsAppGateway gateway;

  @Override
  public OutboxChannel channel() {
    return OutboxChannel.WHATSAPP;
  }

  @Override
  public String deliver(OutboxMessageId id, String recipient, String payload) {
    var content = read(payload);
    if (content.variables() == null
        || content.variables().size() != content.template().variables()) {
      throw new PermanentDeliveryException("Wrong variables for " + content.template());
    }
    var connection =
        connections
            .findById(new PartyId(content.organizationId()))
            .filter(c -> c.isConnected() && c.getPhoneNumberId() != null)
            .orElseThrow(
                () -> new PermanentDeliveryException("The church has no WhatsApp connected"));
    return gateway.sendTemplate(
        connection.getPhoneNumberId(),
        digitsOnly(recipient),
        content.template().metaName(),
        WhatsAppTemplate.LANGUAGE,
        content.variables());
  }

  private static WhatsAppPayload read(String payload) {
    try {
      var content = JSON.readValue(payload, WhatsAppPayload.class);
      if (content.organizationId() == null || content.template() == null) {
        throw new PermanentDeliveryException("Incomplete WhatsApp content");
      }
      return content;
    } catch (JsonProcessingException | IllegalArgumentException | NullPointerException e) {
      // Not worth repeating: the same payload will not read the next time either.
      throw new PermanentDeliveryException("Unreadable WhatsApp content", e);
    }
  }

  /** The Cloud API wants the number in international format without the plus sign. */
  static String digitsOnly(String phone) {
    return phone.replaceAll("\\D", "");
  }
}
