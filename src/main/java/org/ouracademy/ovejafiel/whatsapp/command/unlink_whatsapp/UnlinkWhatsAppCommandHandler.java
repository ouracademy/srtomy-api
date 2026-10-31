package org.ouracademy.ovejafiel.whatsapp.command.unlink_whatsapp;

/**
 * The person stops using their WhatsApp as their account; the number goes back to being a guess.
 */
public interface UnlinkWhatsAppCommandHandler {
  UnlinkWhatsAppResult handle(UnlinkWhatsAppCommand command);
}
