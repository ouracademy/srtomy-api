package org.ouracademy.ovejafiel.billing.command.start_whatsapp_trial;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** The church of {@code organizationId} is about to connect its WhatsApp. */
public record StartWhatsappTrialCommand(PartyId organizationId) {}
