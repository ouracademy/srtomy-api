package org.ouracademy.ovejafiel.billing.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.billing.domain.BillingCountry;
import org.ouracademy.ovejafiel.billing.domain.Feature;
import org.ouracademy.ovejafiel.billing.domain.Plan;
import org.ouracademy.ovejafiel.billing.domain.SubscriptionStatus;
import org.ouracademy.ovejafiel.billing.query.get_billing.GetBillingResult;

/**
 * @param maxPeople null when the plan has no maximum (and so for {@code maxLocations})
 */
public record BillingResponse(
    UUID churchId,
    Plan plan,
    SubscriptionStatus status,
    LocalDate paidUntil,
    BillingCountry country,
    long people,
    long locations,
    Integer maxPeople,
    Integer maxLocations,
    List<FeatureResponse> features,
    LocalDateTime whatsappTrialEndsAt,
    int whatsappTrialDays,
    int includedPeople,
    int blockSize,
    List<PriceResponse> prices) {

  public record FeatureResponse(Feature feature, String label, boolean enabled) {}

  public record PriceResponse(
      BillingCountry country,
      String currency,
      BigDecimal monthly,
      BigDecimal yearly,
      BigDecimal extraBlock) {}

  public static BillingResponse from(GetBillingResult result) {
    return new BillingResponse(
        result.churchId().value(),
        result.plan(),
        result.status(),
        result.paidUntil(),
        result.country(),
        result.people(),
        result.locations(),
        result.maxPeople(),
        result.maxLocations(),
        result.features().stream()
            .map(f -> new FeatureResponse(f.feature(), f.label(), f.enabled()))
            .toList(),
        result.whatsappTrialEndsAt(),
        result.whatsappTrialDays(),
        result.includedPeople(),
        result.blockSize(),
        result.prices().stream()
            .map(
                p ->
                    new PriceResponse(
                        p.country(), p.currency(), p.monthly(), p.yearly(), p.extraBlock()))
            .toList());
  }
}
