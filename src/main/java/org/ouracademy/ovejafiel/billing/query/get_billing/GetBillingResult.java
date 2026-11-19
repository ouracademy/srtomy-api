package org.ouracademy.ovejafiel.billing.query.get_billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.ouracademy.ovejafiel.billing.domain.BillingCountry;
import org.ouracademy.ovejafiel.billing.domain.Feature;
import org.ouracademy.ovejafiel.billing.domain.Plan;
import org.ouracademy.ovejafiel.billing.domain.SubscriptionStatus;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * The plan of the church: what it has, what it uses, and every plan with its price.
 *
 * @param plan the plan in force today ({@code FREE} once the paid time and its grace days ran out)
 * @param maxPeople null when the plan has no maximum (and so for {@code maxLocations})
 * @param country where the church paid from last time; null if it never paid
 * @param ownNumber the own WhatsApp number is in force: bought as an extra, or included in the plan
 */
public record GetBillingResult(
    PartyId churchId,
    Plan plan,
    SubscriptionStatus status,
    LocalDate paidUntil,
    BillingCountry country,
    boolean ownNumber,
    long people,
    long locations,
    Integer maxPeople,
    Integer maxLocations,
    List<FeatureState> features,
    List<PlanResult> plans,
    AiResult ai,
    RemindersResult reminders) {

  public record FeatureState(Feature feature, String label, boolean enabled) {}

  /**
   * One plan as the screen of plans shows it.
   *
   * @param fits whether the plan includes the people and locations the church has today
   * @param prices empty for the free plan
   */
  public record PlanResult(
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
      List<PriceResult> prices) {}

  /**
   * @param ownNumberMonthly what the extra own number adds to a month
   * @param ownNumberYearly what it adds to a year
   */
  public record PriceResult(
      BillingCountry country,
      String currency,
      BigDecimal monthly,
      BigDecimal yearly,
      BigDecimal ownNumberMonthly,
      BigDecimal ownNumberYearly) {}

  /**
   * @param used messages the assistant answered this month
   * @param limit messages the plan allows a month
   * @param costUsd estimated cost of the month, in dollars
   * @param maxCostUsd estimated cost at which the assistant stops for the month
   */
  public record AiResult(
      LocalDate month, int used, int limit, BigDecimal costUsd, BigDecimal maxCostUsd) {}

  /**
   * @param used WhatsApp reminders queued this month
   * @param limit reminders a month the plan allows
   */
  public record RemindersResult(int used, int limit) {}
}
