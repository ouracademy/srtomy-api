package org.ouracademy.ovejafiel.billing.query.get_billing;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/** The plan of the church of {@code organizationId}: what it has, what it uses and the prices. */
public record GetBillingQuery(PartyId organizationId) {}
