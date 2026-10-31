package org.ouracademy.ovejafiel.whatsapp.command.link_whatsapp;

/**
 * Proves that a number belongs to the account that holds the code: the code was shown to a
 * signed-in person and is sent from the number, so whoever did both has the account and the phone.
 */
public interface LinkWhatsAppCommandHandler {
  LinkWhatsAppResult handle(LinkWhatsAppCommand command);
}
