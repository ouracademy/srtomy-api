package org.ouracademy.ovejafiel.user.command.resolve_whatsapp_user;

/**
 * Finds the user linked to the person, or makes the shadow user for WhatsApp and links it. A shadow
 * user is capped at the Member level (see {@code V1.20.0__whatsapp_identity.sql}).
 */
public interface ResolveWhatsAppUserCommandHandler {
  ResolveWhatsAppUserResult handle(ResolveWhatsAppUserCommand command);
}
