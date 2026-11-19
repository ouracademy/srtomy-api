package org.ouracademy.ovejafiel.billing.controller.dto;

import jakarta.validation.constraints.NotNull;
import org.ouracademy.ovejafiel.billing.domain.BillingCountry;
import org.ouracademy.ovejafiel.billing.domain.Plan;

/**
 * What to pay.
 *
 * @param plan The plan to pay: BASIC, CHURCH or NETWORK. It must include the people and locations
 *     the church has today.
 * @param country Where the church pays from: PE (soles) or CO (Colombian pesos).
 * @param months 1 for one month or 12 for one year (a year costs ten months).
 * @param ownNumber True to add the own WhatsApp number as an extra (BASIC and CHURCH only: NETWORK
 *     includes it). Omitted means false.
 */
public record CheckoutRequest(
    @NotNull Plan plan,
    @NotNull BillingCountry country,
    @NotNull Integer months,
    Boolean ownNumber) {}
