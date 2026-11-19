package org.ouracademy.ovejafiel.billing.command.start_checkout;

import org.ouracademy.ovejafiel.billing.domain.BillingCountry;
import org.ouracademy.ovejafiel.billing.domain.Plan;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * The church of {@code organizationId} wants to pay {@code months} (1 or 12) of {@code plan} from
 * {@code country}, with the own WhatsApp number as an extra if {@code ownNumber}.
 */
public record StartCheckoutCommand(
    PartyId organizationId, Plan plan, BillingCountry country, int months, boolean ownNumber) {}
