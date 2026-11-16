package org.ouracademy.ovejafiel.billing.domain;

import java.math.BigDecimal;
import java.util.Map;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

/**
 * The plans in numbers: what the free plan includes and what the paid one costs. The paid plan has
 * a base price that includes {@code includedPeople} people, and adds {@code extraBlockPrice} for
 * every {@code blockSize} people (or part of it) above that. Paying a year costs {@code
 * yearlyMonthsCharged} months.
 */
public record BillingPolicy(
    int freeMaxPeople,
    int freeMaxLocations,
    int whatsappTrialDays,
    int includedPeople,
    int blockSize,
    int yearlyMonthsCharged,
    int graceDays,
    Map<BillingCountry, Price> prices) {

  /** Monthly prices of a country, in its currency. */
  public record Price(BigDecimal base, BigDecimal extraBlock) {}

  /** What a period costs for a church with {@code people} people. */
  public record Quote(
      BillingCountry country, String currency, BigDecimal monthly, BigDecimal total, int months) {}

  /** Blocks of people above the ones the base price includes. */
  public int extraBlocks(long people) {
    if (people <= includedPeople) {
      return 0;
    }
    return (int) Math.ceil((people - includedPeople) / (double) blockSize);
  }

  public BigDecimal monthlyPrice(BillingCountry country, long people) {
    var price = prices.get(country);
    if (price == null) {
      throw new DomainValidationException("Aún no cobramos en ese país");
    }
    return price.base().add(price.extraBlock().multiply(BigDecimal.valueOf(extraBlocks(people))));
  }

  /** Only a month or a year can be paid at once. */
  public Quote quote(BillingCountry country, long people, int months) {
    if (months != 1 && months != 12) {
      throw new DomainValidationException("Solo se puede pagar un mes o un año");
    }
    var monthly = monthlyPrice(country, people);
    var charged = months == 12 ? yearlyMonthsCharged : months;
    return new Quote(
        country,
        country.currency(),
        monthly,
        monthly.multiply(BigDecimal.valueOf(charged)),
        months);
  }
}
