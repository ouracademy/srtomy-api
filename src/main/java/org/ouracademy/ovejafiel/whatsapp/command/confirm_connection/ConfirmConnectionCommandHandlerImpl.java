package org.ouracademy.ovejafiel.whatsapp.command.confirm_connection;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.whatsapp.gateway.WhatsAppGateway;
import org.ouracademy.ovejafiel.whatsapp.gateway.WhatsAppGateway.PhoneInfo;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppWebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
class ConfirmConnectionCommandHandlerImpl implements ConfirmConnectionCommandHandler {

  private final WhatsAppConnectionRepository connections;
  private final WhatsAppWebhookEventRepository webhookEvents;
  private final WhatsAppGateway gateway;
  private final TransactionTemplate transaction;

  /**
   * The provider is asked first, outside any transaction: a slow answer must not hold a database
   * connection (the pool is small). Only the writes are one transaction.
   */
  @Override
  public ConfirmConnectionResult handle(ConfirmConnectionCommand command) {
    var info = phoneInfo(command.phoneNumberId());
    return transaction.execute(status -> confirm(command, info));
  }

  private ConfirmConnectionResult confirm(ConfirmConnectionCommand command, PhoneInfo info) {
    var now = LocalDateTime.now();
    if (command.idempotencyKey() != null
        && !webhookEvents.markReceived(command.idempotencyKey(), now)) {
      return ConfirmConnectionResult.ALREADY_HANDLED;
    }
    var connection = connections.findByKapsoCustomerId(command.customerId()).orElse(null);
    if (connection == null) {
      return ConfirmConnectionResult.UNKNOWN_CUSTOMER;
    }
    connection.markConnected(
        command.phoneNumberId(), info.displayNumber(), info.businessAccountId(), now);
    connections.save(connection);
    return ConfirmConnectionResult.CONNECTED;
  }

  /** Only to show the number and to create templates later: the connection is made without it. */
  private PhoneInfo phoneInfo(String phoneNumberId) {
    try {
      var info = gateway.phoneInfo(phoneNumberId);
      return info == null ? new PhoneInfo(null, null) : info;
    } catch (RuntimeException e) {
      return new PhoneInfo(null, null);
    }
  }
}
