package org.ouracademy.ovejafiel.billing.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.billing.domain.BillingCountry;
import org.ouracademy.ovejafiel.billing.domain.Feature;
import org.ouracademy.ovejafiel.billing.domain.Plan;
import org.ouracademy.ovejafiel.billing.domain.SubscriptionStatus;
import org.ouracademy.ovejafiel.billing.query.get_billing.GetBillingResult;

/**
 * The plan of a church.
 *
 * @param plan the plan in force today: FREE, BASIC, CHURCH or NETWORK (FREE again once the paid
 *     time and its grace days ran out)
 * @param maxPeople null when the plan has no maximum (and so for {@code maxLocations})
 * @param ownNumber the own WhatsApp number is in force (bought as an extra, or included)
 * @param plans every plan with what it includes and what it costs, to choose one
 * @param ai what the AI assistant used this month: messages against the limit of the plan, and the
 *     estimated cost in dollars against the cap at which it stops
 * @param reminders the WhatsApp reminders (Meta templates) queued this month against the plan
 */
public record BillingResponse(
    UUID churchId,
    Plan plan,
    SubscriptionStatus status,
    LocalDate paidUntil,
    BillingCountry country,
    boolean ownNumber,
    long people,
    long locations,
    Integer maxPeople,
    Integer maxLocations,
    List<FeatureResponse> features,
    List<PlanResponse> plans,
    AiResponse ai,
    RemindersResponse reminders) {

  public record FeatureResponse(Feature feature, String label, boolean enabled) {}

  /**
   * @param fits whether the plan includes the people and locations the church has today
   * @param prices empty for the free plan
   */
  public record PlanResponse(
      Plan plan,
      String label,
      Integer maxPeople,
      Integer maxLocations,
      int aiMessages,
      int whatsappReminders,
      boolean ownNumberIncluded,
      boolean ownNumberExtra,
      boolean customRoles,
      boolean excelExport,
      boolean churchFeatures,
      boolean fits,
      List<PriceResponse> prices) {}

  /**
   * @param ownNumberMonthly what the extra own number adds to a month
   * @param ownNumberYearly what it adds to a year
   */
  public record PriceResponse(
      BillingCountry country,
      String currency,
      BigDecimal monthly,
      BigDecimal yearly,
      BigDecimal ownNumberMonthly,
      BigDecimal ownNumberYearly) {}

  public record AiResponse(
      LocalDate month, int used, int limit, BigDecimal costUsd, BigDecimal maxCostUsd) {}

  public record RemindersResponse(int used, int limit) {}

  public static BillingResponse from(GetBillingResult result) {
    return new BillingResponse(
        result.churchId().value(),
        result.plan(),
        result.status(),
        result.paidUntil(),
        result.country(),
        result.ownNumber(),
        result.people(),
        result.locations(),
        result.maxPeople(),
        result.maxLocations(),
        result.features().stream()
            .map(f -> new FeatureResponse(f.feature(), f.label(), f.enabled()))
            .toList(),
        result.plans().stream()
            .map(
                p ->
                    new PlanResponse(
                        p.plan(),
                        p.label(),
                        p.maxPeople(),
                        p.maxLocations(),
                        p.aiMessages(),
                        p.whatsappReminders(),
                        p.ownNumberIncluded(),
                        p.ownNumberExtra(),
                        p.customRoles(),
                        p.excelExport(),
                        p.churchFeatures(),
                        p.fits(),
                        p.prices().stream()
                            .map(
                                price ->
                                    new PriceResponse(
                                        price.country(),
                                        price.currency(),
                                        price.monthly(),
                                        price.yearly(),
                                        price.ownNumberMonthly(),
                                        price.ownNumberYearly()))
                            .toList()))
            .toList(),
        new AiResponse(
            result.ai().month(),
            result.ai().used(),
            result.ai().limit(),
            result.ai().costUsd(),
            result.ai().maxCostUsd()),
        new RemindersResponse(result.reminders().used(), result.reminders().limit()));
  }
}
