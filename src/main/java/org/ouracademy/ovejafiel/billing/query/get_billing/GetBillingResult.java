package org.ouracademy.ovejafiel.billing.query.get_billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.ouracademy.ovejafiel.billing.domain.BillingCountry;
import org.ouracademy.ovejafiel.billing.domain.Feature;
import org.ouracademy.ovejafiel.billing.domain.Plan;
import org.ouracademy.ovejafiel.billing.domain.SubscriptionStatus;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * @param maxPeople null when the plan has no maximum (and so for {@code maxLocations})
 * @param country where the church paid from last time; null if it never paid
 * @param prices what the paid plan costs today for the people the church has, per country
 */
public record GetBillingResult(
    PartyId churchId,
    Plan plan,
    SubscriptionStatus status,
    LocalDate paidUntil,
    BillingCountry country,
    long people,
    long locations,
    Integer maxPeople,
    Integer maxLocations,
    List<FeatureState> features,
    LocalDateTime whatsappTrialEndsAt,
    int whatsappTrialDays,
    int includedPeople,
    int blockSize,
    List<PriceResult> prices) {

  public record FeatureState(Feature feature, String label, boolean enabled) {}

  public record PriceResult(
      BillingCountry country,
      String currency,
      BigDecimal monthly,
      BigDecimal yearly,
      BigDecimal extraBlock) {}
}
