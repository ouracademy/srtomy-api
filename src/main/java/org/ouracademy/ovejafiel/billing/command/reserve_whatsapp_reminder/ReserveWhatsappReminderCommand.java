package org.ouracademy.ovejafiel.billing.command.reserve_whatsapp_reminder;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * A WhatsApp reminder (a Meta template) is about to be queued for the church of {@code
 * organizationId} (any node of its tree): one of the month is counted if the plan still has them.
 */
public record ReserveWhatsappReminderCommand(PartyId organizationId) {}
