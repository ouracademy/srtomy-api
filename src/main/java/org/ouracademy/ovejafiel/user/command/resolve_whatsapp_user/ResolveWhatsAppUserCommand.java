package org.ouracademy.ovejafiel.user.command.resolve_whatsapp_user;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** The user that stands for {@code personId} when they write on WhatsApp. */
public record ResolveWhatsAppUserCommand(PartyId personId, PartyId churchId) {}
