package org.ouracademy.ovejafiel.whatsapp.command.update_connection_state;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppWebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps our record of a church's number in step with the provider. Without it the portal would keep
 * saying "connected" for a number that cannot send or receive, and the reminders would fail one by
 * one. Everything that sends or answers already looks at the status, so marking the connection is
 * what stops them.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class UpdateConnectionStateCommandHandlerImpl implements UpdateConnectionStateCommandHandler {

  private final WhatsAppConnectionRepository connections;
  private final WhatsAppWebhookEventRepository webhookEvents;

  @Override
  @Transactional
  public UpdateConnectionStateResult handle(UpdateConnectionStateCommand command) {
    var now = LocalDateTime.now();
    if (command.idempotencyKey() != null
        && !webhookEvents.markReceived(command.idempotencyKey(), now)) {
      return new UpdateConnectionStateResult(0, true);
    }
    var changed = 0;
    for (var phoneNumberId : command.phoneNumberIds()) {
      var connection = connections.findByPhoneNumberId(phoneNumberId).orElse(null);
      if (connection == null) {
        continue;
      }
      if (command.reason() == null) {
        if (connection.restore()) {
          changed++;
          connections.save(connection);
          log.info("WhatsApp of {} works again", connection.getOrganizationId());
        }
      } else if (connection.isConnected()) {
        connection.lose(command.reason());
        connections.save(connection);
        changed++;
        log.warn("WhatsApp of {} lost: {}", connection.getOrganizationId(), command.reason());
      }
    }
    return new UpdateConnectionStateResult(changed, false);
  }
}
