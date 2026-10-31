package org.ouracademy.ovejafiel.user.command.resolve_whatsapp_user;

/**
 * @param subject the subject of the user that represents the person
 * @param realAccount true when it is a user who signs in (a real account); false for the shadow
 *     user made for WhatsApp, which only has the Member level
 */
public record ResolveWhatsAppUserResult(String subject, boolean realAccount) {}
