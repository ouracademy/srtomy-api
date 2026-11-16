package org.ouracademy.ovejafiel.billing.command.reserve_ai_message;

import static org.ouracademy.ovejafiel.billing.command.reserve_ai_message.ReserveAiMessageResult.PLAN;
import static org.ouracademy.ovejafiel.billing.command.reserve_ai_message.ReserveAiMessageResult.QUOTA;
import static org.ouracademy.ovejafiel.billing.command.reserve_ai_message.ReserveAiMessageResult.SPEND;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.domain.AiChannel;
import org.ouracademy.ovejafiel.billing.domain.AiUsageId;
import org.ouracademy.ovejafiel.billing.domain.Feature;
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
    if (command.channel() == AiChannel.WHATSAPP && !state.allows(Feature.WHATSAPP)) {
      return denied(PLAN, allowance, state.isPaid());
    }
    if (allowance.limit() <= 0 || allowance.maxCostUsd().compareTo(BigDecimal.ZERO) <= 0) {
      return denied(QUOTA, allowance, state.isPaid());
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
      return denied(now.quotaReached() ? QUOTA : SPEND, now, state.isPaid());
    }
    var used = counted.getFirst();
    return new ReserveAiMessageResult(
        true, null, used, allowance.limit(), Math.max(0, allowance.limit() - used), null);
  }

  private static ReserveAiMessageResult denied(
      String reason, AiAllowance.Allowance allowance, boolean paid) {
    return new ReserveAiMessageResult(
        false,
        reason,
        allowance.used(),
        allowance.limit(),
        allowance.remaining(),
        message(reason, allowance, paid));
  }

  private static String message(String reason, AiAllowance.Allowance allowance, boolean paid) {
    return switch (reason) {
      case PLAN ->
          "El asistente de WhatsApp está en pausa porque terminó la prueba gratuita. Pasa al plan"
              + " Iglesia para seguir usándolo.";
      case QUOTA ->
          paid
              ? "Tu iglesia usó los "
                  + allowance.limit()
                  + " mensajes del asistente de este mes. Se renuevan el próximo mes."
              : "Tu iglesia usó los "
                  + allowance.limit()
                  + " mensajes gratis del asistente de este mes. Pasa al plan Iglesia para tener"
                  + " más, o espera al próximo mes.";
      default ->
          "El asistente llegó al límite de uso de tu iglesia para este mes. Se renueva el próximo"
              + " mes.";
    };
  }
}
