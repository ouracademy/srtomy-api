package org.ouracademy.ovejafiel.whatsapp.command.provision_templates;

/**
 * Submits to Meta the message templates the app sends, for the church's WhatsApp account. Asked by
 * the pastor, once after connecting; running it again is harmless (what exists is refused and
 * reported). Meta reviews each template, usually within a day.
 */
public interface ProvisionTemplatesCommandHandler {
  ProvisionTemplatesResult handle(ProvisionTemplatesCommand command);
}
