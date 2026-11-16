package org.ouracademy.ovejafiel.billing.query.list_invoices;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** The payments of the church of {@code organizationId}, the newest first. */
public record ListInvoicesQuery(PartyId organizationId) {}
