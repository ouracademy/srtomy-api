package org.ouracademy.ovejafiel.billing.command.reserve_whatsapp_reminder;

public interface ReserveWhatsappReminderCommandHandler {
  ReserveWhatsappReminderResult handle(ReserveWhatsappReminderCommand command);
}
