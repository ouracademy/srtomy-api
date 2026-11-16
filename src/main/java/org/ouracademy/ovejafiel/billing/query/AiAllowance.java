package org.ouracademy.ovejafiel.billing.query;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.domain.AiPolicy;
import org.ouracademy.ovejafiel.billing.domain.AiUsage;
import org.ouracademy.ovejafiel.billing.domain.BillingCountry;
import org.ouracademy.ovejafiel.billing.domain.BillingPolicy;
import org.ouracademy.ovejafiel.billing.repository.AiUsageRepository;
import org.ouracademy.ovejafiel.person.query.count_people.CountPeopleQuery;
import org.ouracademy.ovejafiel.person.query.count_people.CountPeopleQueryHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * What the AI assistant may still answer this month for a church: the caps of its plan and what it
 * used. Months follow the time of Lima and Bogota ({@code billing.ai.zone}), where the churches
 * are.
 */
@Component
@RequiredArgsConstructor
public class AiAllowance {

  private final AiPolicy aiPolicy;
  private final BillingPolicy billingPolicy;
  private final AiUsageRepository usages;
  private final CountPeopleQueryHandler countPeople;
  private final Clock clock;

  @Value("${billing.ai.zone:America/Lima}")
  private String zone;

  /**
   * @param month first day of the current month
   */
  public record Allowance(
      LocalDate month, int used, int limit, BigDecimal costUsd, BigDecimal maxCostUsd) {

    public int remaining() {
      return Math.max(0, limit - used);
    }

    public boolean quotaReached() {
      return used >= limit;
    }

    public boolean spendReached() {
      return costUsd.compareTo(maxCostUsd) >= 0;
    }
  }

  public LocalDate currentMonth() {
    return LocalDate.now(clock.withZone(ZoneId.of(zone))).withDayOfMonth(1);
  }

  public Allowance of(Entitlements.State state) {
    return of(state, countPeople.handle(new CountPeopleQuery(state.churchId())).count());
  }

  public Allowance of(Entitlements.State state, long people) {
    var month = currentMonth();
    var usage = usages.findByOrganizationIdAndMonth(state.churchId(), month);
    var paid = state.isPaid();
    var country =
        state.subscription() == null || state.subscription().getCountry() == null
            ? BillingCountry.PE
            : state.subscription().getCountry();
    return new Allowance(
        month,
        usage.map(AiUsage::getMessages).orElse(0),
        aiPolicy.messageLimit(paid, billingPolicy.extraBlocks(people)),
        usage.map(AiUsage::getCostUsd).orElse(BigDecimal.ZERO),
        aiPolicy.maxCostUsd(paid, country, billingPolicy.monthlyPrice(country, people)));
  }
}
