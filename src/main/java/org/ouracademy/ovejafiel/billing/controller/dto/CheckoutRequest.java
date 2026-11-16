package org.ouracademy.ovejafiel.billing.controller.dto;

import jakarta.validation.constraints.NotNull;
import org.ouracademy.ovejafiel.billing.domain.BillingCountry;

/**
 * What to pay.
 *
 * @param country Where the church pays from: PE (soles) or CO (Colombian pesos).
 * @param months 1 for one month or 12 for one year (a year costs ten months).
 */
public record CheckoutRequest(@NotNull BillingCountry country, @NotNull Integer months) {}
