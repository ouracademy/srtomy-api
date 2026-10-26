package org.ouracademy.ovejafiel.notification.command.send_email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.ovejafiel.notification.gateway.EmailGateway;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class SendEmailCommandHandlerImpl implements SendEmailCommandHandler {

  private final EmailGateway emailGateway;

  @Override
  public SendEmailResult handle(SendEmailCommand command) {
    try {
      emailGateway.send(command.to(), command.subject(), command.text());
      return new SendEmailResult(true);
    } catch (RuntimeException e) {
      // The caller keeps its own work (the invitation exists and can be resent).
      log.error("Could not send the email '{}' to {}", command.subject(), command.to(), e);
      return new SendEmailResult(false);
    }
  }
}
