package org.ouracademy.ovejafiel.notification.command.send_email;

/** Never throws because of the provider: a failed delivery is reported in the result. */
public interface SendEmailCommandHandler {
  SendEmailResult handle(SendEmailCommand command);
}
