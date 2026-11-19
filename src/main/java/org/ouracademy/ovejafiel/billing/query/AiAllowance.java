package org.ouracademy.ovejafiel.billing.query;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.domain.AiUsage;
import org.ouracademy.ovejafiel.billing.repository.AiUsageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * What a church may still use this month of what its plan gives: messages of the AI assistant (and
 * the dollars they may cost) and WhatsApp reminders. Months follow the time of Lima and Bogota
 * ({@code billing.ai.zone}), where the churches are.
 */
@Component
@RequiredArgsConstructor
public class AiAllowance {

  private final AiUsageRepository usages;
  private final Clock clock;

  @Value("${billing.ai.zone:America/Lima}")
  private String zone;

  /**
   * @param month first day of the current month
   */
  public record Allowance(
      LocalDate month,
      int used,
      int limit,
      BigDecimal costUsd,
      BigDecimal maxCostUsd,
      int remindersUsed,
      int remindersLimit) {

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
    var month = currentMonth();
    var usage = usages.findByOrganizationIdAndMonth(state.churchId(), month);
    var limits = state.limits();
    return new Allowance(
        month,
        usage.map(AiUsage::getMessages).orElse(0),
        limits.aiMessages(),
        usage.map(AiUsage::getCostUsd).orElse(BigDecimal.ZERO),
        limits.aiMaxCostUsd(),
        usage.map(AiUsage::getWhatsappReminders).orElse(0),
        limits.whatsappReminders());
  }
}
