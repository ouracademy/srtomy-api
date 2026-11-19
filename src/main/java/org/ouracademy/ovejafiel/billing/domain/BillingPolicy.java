package org.ouracademy.ovejafiel.billing.domain;

import java.math.BigDecimal;
import java.util.Map;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

/**
 * The plans in numbers: what each includes and what it costs a month in each country (taxes
 * included). There is no price per block of people: every plan has its own maximum, and a church
 * that reaches it moves to the next plan. Paying a year costs {@code yearlyMonthsCharged} months.
 * The own WhatsApp number is an extra for the plans that do not include it.
 */
public record BillingPolicy(
    Map<Plan, Limits> limits,
    Map<Plan, Map<BillingCountry, BigDecimal>> prices,
    Map<BillingCountry, BigDecimal> ownNumberPrice,
    int yearlyMonthsCharged,
    int graceDays) {

  /**
   * What a plan includes. A null maximum means no maximum.
   *
   * @param aiMaxCostUsd the estimated cost of the assistant, in dollars a month, at which it stops
   * @param whatsappReminders reminders by WhatsApp template a month
   * @param ownNumberIncluded the own WhatsApp number comes with the plan
   * @param ownNumberExtra the own WhatsApp number can be bought as an extra
   * @param churchFeatures mass email, finances, shifts and volunteers
   */
  public record Limits(
      Integer maxPeople,
      Integer maxLocations,
      int aiMessages,
      BigDecimal aiMaxCostUsd,
      int whatsappReminders,
      boolean ownNumberIncluded,
      boolean ownNumberExtra,
      boolean customRoles,
      boolean excelExport,
      boolean churchFeatures) {

    /** Whether the plan includes the feature (the paid extra of the own number is apart). */
    public boolean includes(Feature feature) {
      return switch (feature) {
        case MASS_EMAIL, FINANCES, VOLUNTEERS -> churchFeatures;
        case EXCEL_EXPORT -> excelExport;
        case CUSTOM_ROLES -> customRoles;
        case OWN_NUMBER -> ownNumberIncluded;
      };
    }

    public boolean allowsPeople(long people) {
      return maxPeople == null || people <= maxPeople;
    }

    public boolean allowsLocations(long locations) {
      return maxLocations == null || locations <= maxLocations;
    }
  }

  /** What a period costs. {@code monthly} is the price of one month, extra included. */
  public record Quote(
      Plan plan,
      BillingCountry country,
      String currency,
      BigDecimal monthly,
      BigDecimal total,
      int months,
      boolean ownNumber) {}

  public Limits limits(Plan plan) {
    return limits.get(plan);
  }

  /** The monthly price of a paid plan in a country, without the extra. */
  public BigDecimal monthlyPrice(Plan plan, BillingCountry country) {
    var price = prices.getOrDefault(plan, Map.of()).get(country);
    if (price == null) {
      throw new DomainValidationException("Aún no cobramos ese plan en ese país");
    }
    return price;
  }

  /** What the extra own number costs a month in a country. */
  public BigDecimal ownNumberMonthly(BillingCountry country) {
    var price = ownNumberPrice.get(country);
    if (price == null) {
      throw new DomainValidationException("Aún no cobramos ese plan en ese país");
    }
    return price;
  }

  /** Only a month or a year can be paid at once. */
  public Quote quote(Plan plan, BillingCountry country, int months, boolean ownNumber) {
    if (!plan.isPaid()) {
      throw new DomainValidationException("El plan Gratis no se paga");
    }
    if (months != 1 && months != 12) {
      throw new DomainValidationException("Solo se puede pagar un mes o un año");
    }
    if (ownNumber && !limits(plan).ownNumberExtra()) {
      throw new DomainValidationException(
          limits(plan).ownNumberIncluded()
              ? "El plan " + plan.label() + " ya incluye el número de WhatsApp propio"
              : "El plan " + plan.label() + " no permite número de WhatsApp propio");
    }
    var monthly = monthlyPrice(plan, country);
    if (ownNumber) {
      monthly = monthly.add(ownNumberMonthly(country));
    }
    var charged = months == 12 ? yearlyMonthsCharged : months;
    return new Quote(
        plan,
        country,
        country.currency(),
        monthly,
        monthly.multiply(BigDecimal.valueOf(charged)),
        months,
        ownNumber);
  }
}
