package org.ouracademy.ovejafiel.whatsapp.command.create_link_code;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** {@code subject} asks for a code to link their WhatsApp; {@code organizationId} is any node. */
public record CreateLinkCodeCommand(String subject, PartyId organizationId) {}
