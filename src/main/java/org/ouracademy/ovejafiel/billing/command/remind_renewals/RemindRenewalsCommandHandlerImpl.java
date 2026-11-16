package org.ouracademy.ovejafiel.billing.command.remind_renewals;

import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.command.RenewalReminderSender;
import org.ouracademy.ovejafiel.billing.domain.RenewalReminder;
import org.ouracademy.ovejafiel.billing.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

/**
 * Each church in its own transaction ({@link RenewalReminderSender}): a church whose email cannot
 * be queued does not stop the others, and Cloud Scheduler can run the job again safely.
 */
@Service
@RequiredArgsConstructor
class RemindRenewalsCommandHandlerImpl implements RemindRenewalsCommandHandler {

  private final SubscriptionRepository subscriptions;
  private final RenewalReminderSender sender;
  private final Clock clock;

  @Override
  public RemindRenewalsResult handle(RemindRenewalsCommand command) {
    var today = LocalDate.now(clock);
    var churches = 0;
    var emails = 0;
    for (var kind : RenewalReminder.values()) {
      for (var subscription : subscriptions.findByPaidUntil(kind.paidUntil(today))) {
        var sent =
            sender.remind(subscription.getOrganizationId(), subscription.getPaidUntil(), kind);
        if (sent >= 0) {
          churches++;
          emails += sent;
        }
      }
    }
    return new RemindRenewalsResult(churches, emails);
  }
}
