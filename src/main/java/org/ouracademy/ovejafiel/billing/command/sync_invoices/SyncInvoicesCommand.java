package org.ouracademy.ovejafiel.billing.command.sync_invoices;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** Check with the gateway every payment the church of {@code organizationId} has pending. */
public record SyncInvoicesCommand(PartyId organizationId) {}
