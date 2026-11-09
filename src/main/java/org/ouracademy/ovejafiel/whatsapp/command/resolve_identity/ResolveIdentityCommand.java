package org.ouracademy.ovejafiel.whatsapp.command.resolve_identity;

/**
 * Who wrote: {@code phoneNumberId} is the church number they wrote to, {@code from} the number
 * WhatsApp says the message comes from.
 */
public record ResolveIdentityCommand(String phoneNumberId, String from) {}
