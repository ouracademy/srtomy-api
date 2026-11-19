package org.ouracademy.ovejafiel.billing.command.reserve_whatsapp_reminder;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.domain.AiUsageId;
import org.ouracademy.ovejafiel.billing.query.AiAllowance;
import org.ouracademy.ovejafiel.billing.query.Entitlements;
import org.ouracademy.ovejafiel.billing.repository.AiUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks and counts in one statement ({@link AiUsageRepository#reserveReminder}), so two jobs
 * queueing reminders at the same time never go past the plan. Runs in the transaction of the caller
 * (the reminder is queued in it), so a reminder that is not queued is not counted either.
 */
@Service
@RequiredArgsConstructor
class ReserveWhatsappReminderCommandHandlerImpl implements ReserveWhatsappReminderCommandHandler {

  private final Entitlements entitlements;
  private final AiAllowance allowance;
  private final AiUsageRepository usages;

  @Override
  @Transactional
  public ReserveWhatsappReminderResult handle(ReserveWhatsappReminderCommand command) {
    var state = entitlements.of(command.organizationId());
    var limit = state.limits().whatsappReminders();
    var counted =
        usages.reserveReminder(
            AiUsageId.generate().value(),
            state.churchId().value(),
            allowance.currentMonth(),
            limit);
    if (counted.isEmpty()) {
      return new ReserveWhatsappReminderResult(false, limit, limit);
    }
    return new ReserveWhatsappReminderResult(true, counted.getFirst(), limit);
  }
}
