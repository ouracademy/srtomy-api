package org.ouracademy.ovejafiel.person.command.set_whatsapp_consent;

/** Turns WhatsApp notices on or off for people, e.g. when one of them answers STOP. */
public interface SetWhatsAppConsentCommandHandler {
  SetWhatsAppConsentResult handle(SetWhatsAppConsentCommand command);
}
