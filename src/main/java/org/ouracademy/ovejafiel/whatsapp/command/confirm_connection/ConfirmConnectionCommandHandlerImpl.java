package org.ouracademy.ovejafiel.whatsapp.command.confirm_connection;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.whatsapp.gateway.WhatsAppGateway;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppWebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ConfirmConnectionCommandHandlerImpl implements ConfirmConnectionCommandHandler {

  private final WhatsAppConnectionRepository connections;
  private final WhatsAppWebhookEventRepository webhookEvents;
  private final WhatsAppGateway gateway;

  @Override
  @Transactional
  public ConfirmConnectionResult handle(ConfirmConnectionCommand command) {
    var now = LocalDateTime.now();
    if (command.idempotencyKey() != null
        && !webhookEvents.markReceived(command.idempotencyKey(), now)) {
      return ConfirmConnectionResult.ALREADY_HANDLED;
    }
    var connection = connections.findByKapsoCustomerId(command.customerId()).orElse(null);
    if (connection == null) {
      return ConfirmConnectionResult.UNKNOWN_CUSTOMER;
    }
    connection.markConnected(command.phoneNumberId(), displayNumber(command.phoneNumberId()), now);
    connections.save(connection);
    return ConfirmConnectionResult.CONNECTED;
  }

  /** Only to show it: if Kapso cannot say, the connection is made anyway without the number. */
  private String displayNumber(String phoneNumberId) {
    try {
      return gateway.displayNumber(phoneNumberId);
    } catch (RuntimeException e) {
      return null;
    }
  }
}
