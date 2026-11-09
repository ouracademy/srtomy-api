package org.ouracademy.ovejafiel.whatsapp.command.confirm_connection;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.ouracademy.ovejafiel.whatsapp.gateway.WhatsAppGateway;
import org.ouracademy.ovejafiel.whatsapp.gateway.WhatsAppGateway.PhoneInfo;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppWebhookEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class ConfirmConnectionCommandHandlerImpl implements ConfirmConnectionCommandHandler {

  private final WhatsAppConnectionRepository connections;
  private final WhatsAppWebhookEventRepository webhookEvents;
  private final WhatsAppGateway gateway;
  private final TransactionTemplate transaction;
  private final String agentWebhookUrl;
  private final String agentWebhookSecret;

  ConfirmConnectionCommandHandlerImpl(
      WhatsAppConnectionRepository connections,
      WhatsAppWebhookEventRepository webhookEvents,
      WhatsAppGateway gateway,
      TransactionTemplate transaction,
      @Value("${whatsapp.agent.webhook-url:}") String agentWebhookUrl,
      @Value("${whatsapp.agent.webhook-secret:}") String agentWebhookSecret) {
    this.connections = connections;
    this.webhookEvents = webhookEvents;
    this.gateway = gateway;
    this.transaction = transaction;
    this.agentWebhookUrl = agentWebhookUrl;
    this.agentWebhookSecret = agentWebhookSecret;
  }

  /**
   * The provider is asked first, outside any transaction: a slow answer must not hold a database
   * connection (the pool is small). Only the writes are one transaction.
   */
  @Override
  public ConfirmConnectionResult handle(ConfirmConnectionCommand command) {
    var info = phoneInfo(command.phoneNumberId());
    var result = transaction.execute(status -> confirm(command, info));
    if (result == ConfirmConnectionResult.CONNECTED
        || result == ConfirmConnectionResult.ALREADY_HANDLED) {
      // Also when the delivery is a retry: if the first try connected the church but failed here,
      // the retry is what finishes it. A failure is not caught on purpose: the provider then
      // delivers the event again, and a church whose number never reaches the agent would be
      // connected in the portal but mute.
      routeMessagesToTheAgent(command.phoneNumberId());
    }
    return result;
  }

  /** Every number needs its own webhook for messages; the provider does not make it by itself. */
  private void routeMessagesToTheAgent(String phoneNumberId) {
    if (agentWebhookUrl.isBlank() || agentWebhookSecret.isBlank()) {
      return;
    }
    gateway.ensureMessageWebhook(phoneNumberId, agentWebhookUrl, agentWebhookSecret);
  }

  private ConfirmConnectionResult confirm(ConfirmConnectionCommand command, PhoneInfo info) {
    var now = LocalDateTime.now(ZoneOffset.UTC);
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
    } catch (RuntimeException _) {
      return new PhoneInfo(null, null);
    }
  }
}
