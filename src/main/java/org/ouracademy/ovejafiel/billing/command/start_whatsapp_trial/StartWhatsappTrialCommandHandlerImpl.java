package org.ouracademy.ovejafiel.billing.command.start_whatsapp_trial;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.domain.BillingPolicy;
import org.ouracademy.ovejafiel.billing.domain.Feature;
import org.ouracademy.ovejafiel.billing.domain.Subscription;
import org.ouracademy.ovejafiel.billing.query.Entitlements;
import org.ouracademy.ovejafiel.billing.repository.SubscriptionRepository;
import org.ouracademy.ovejafiel.shared.domain.PlanLimitException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class StartWhatsappTrialCommandHandlerImpl implements StartWhatsappTrialCommandHandler {

  private final Entitlements entitlements;
  private final SubscriptionRepository subscriptions;
  private final BillingPolicy policy;
  private final Clock clock;

  @Override
  @Transactional
  public StartWhatsappTrialResult handle(StartWhatsappTrialCommand command) {
    var state = entitlements.of(command.organizationId());
    if (state.isPaid()) {
      return new StartWhatsappTrialResult(null);
    }
    if (!state.allows(Feature.WHATSAPP)) {
      throw new PlanLimitException(
          "feature.whatsapp",
          "La prueba gratuita de WhatsApp terminó. Pasa al plan Iglesia para seguir usándolo.");
    }
    var subscription =
        state.subscription() == null ? new Subscription(state.churchId()) : state.subscription();
    subscription.startWhatsappTrial(LocalDateTime.now(clock));
    subscriptions.save(subscription);
    return new StartWhatsappTrialResult(
        subscription.whatsappTrialEndsAt(policy.whatsappTrialDays()));
  }
}
