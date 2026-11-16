package org.ouracademy.ovejafiel.billing.command.start_checkout;

import org.ouracademy.ovejafiel.billing.domain.BillingCountry;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * The church of {@code organizationId} wants to pay {@code months} (1 or 12) of the paid plan from
 * {@code country}.
 */
public record StartCheckoutCommand(PartyId organizationId, BillingCountry country, int months) {}
