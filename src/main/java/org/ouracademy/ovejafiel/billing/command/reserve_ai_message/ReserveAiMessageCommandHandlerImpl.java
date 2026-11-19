package org.ouracademy.ovejafiel.billing.command.reserve_ai_message;

import static org.ouracademy.ovejafiel.billing.command.reserve_ai_message.ReserveAiMessageResult.QUOTA;
import static org.ouracademy.ovejafiel.billing.command.reserve_ai_message.ReserveAiMessageResult.SPEND;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.domain.AiUsageId;
import org.ouracademy.ovejafiel.billing.domain.Plan;
import org.ouracademy.ovejafiel.billing.query.AiAllowance;
import org.ouracademy.ovejafiel.billing.query.Entitlements;
import org.ouracademy.ovejafiel.billing.repository.AiUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks and counts in one statement ({@link AiUsageRepository#reserveMessage}), so two instances
 * of the agent never let a church go past its caps at the same time.
 */
@Service
@RequiredArgsConstructor
class ReserveAiMessageCommandHandlerImpl implements ReserveAiMessageCommandHandler {

  private final Entitlements entitlements;
  private final AiAllowance aiAllowance;
  private final AiUsageRepository usages;

  @Override
  @Transactional
  public ReserveAiMessageResult handle(ReserveAiMessageCommand command) {
    var state = entitlements.of(command.nodeId());
    var allowance = aiAllowance.of(state);
    if (allowance.limit() <= 0 || allowance.maxCostUsd().compareTo(BigDecimal.ZERO) <= 0) {
      return denied(QUOTA, allowance, state.plan());
    }
    var counted =
        usages.reserveMessage(
            AiUsageId.generate().value(),
            state.churchId().value(),
            allowance.month(),
            allowance.limit(),
            allowance.maxCostUsd());
    if (counted.isEmpty()) {
      // Read again: the row may have changed since the allowance was read.
      var now = aiAllowance.of(state);
      return denied(now.quotaReached() ? QUOTA : SPEND, now, state.plan());
    }
    var used = counted.getFirst();
    return new ReserveAiMessageResult(
        true, null, used, allowance.limit(), Math.max(0, allowance.limit() - used), null);
  }

  private static ReserveAiMessageResult denied(
      String reason, AiAllowance.Allowance allowance, Plan plan) {
    return new ReserveAiMessageResult(
        false,
        reason,
        allowance.used(),
        allowance.limit(),
        allowance.remaining(),
        message(reason, allowance, plan));
  }

  private static String message(String reason, AiAllowance.Allowance allowance, Plan plan) {
    var upgrade =
        plan.next()
            .map(n -> " Pasa al plan " + n.label() + " para tener más, o espera")
            .orElse(" Espera");
    return switch (reason) {
      case QUOTA ->
          "Tu iglesia usó los "
              + allowance.limit()
              + " mensajes del asistente que incluye el plan "
              + plan.label()
              + " este mes."
              + upgrade
              + " al próximo mes.";
      default ->
          "El asistente llegó al límite de uso del plan "
              + plan.label()
              + " de tu iglesia este mes."
              + upgrade
              + " al próximo mes.";
    };
  }
}
