package org.ouracademy.ovejafiel.whatsapp.command.unlink_whatsapp;

import org.ouracademy.ovejafiel.party.domain.PartyId;

public record UnlinkWhatsAppCommand(String subject, PartyId organizationId) {}
