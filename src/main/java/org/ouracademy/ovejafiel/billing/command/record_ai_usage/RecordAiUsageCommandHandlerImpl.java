package org.ouracademy.ovejafiel.billing.command.record_ai_usage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.ovejafiel.billing.domain.AiPolicy;
import org.ouracademy.ovejafiel.billing.domain.AiUsageId;
import org.ouracademy.ovejafiel.billing.query.AiAllowance;
import org.ouracademy.ovejafiel.billing.query.Entitlements;
import org.ouracademy.ovejafiel.billing.repository.AiUsageRepository;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
class RecordAiUsageCommandHandlerImpl implements RecordAiUsageCommandHandler {

  private final Entitlements entitlements;
  private final AiAllowance aiAllowance;
  private final AiPolicy aiPolicy;
  private final AiUsageRepository usages;

  @Override
  @Transactional
  public RecordAiUsageResult handle(RecordAiUsageCommand command) {
    if (command.inputTokens() < 0
        || command.outputTokens() < 0
        || command.cacheReadTokens() < 0
        || command.cacheWriteTokens() < 0) {
      throw new DomainValidationException("Los tokens no pueden ser negativos");
    }
    var churchId = entitlements.of(command.nodeId()).churchId();
    var tokens =
        new AiPolicy.Tokens(
            command.inputTokens(),
            command.outputTokens(),
            command.cacheReadTokens(),
            command.cacheWriteTokens());
    var known = aiPolicy.knows(command.model());
    if (!known) {
      log.warn("No price for model {}; priced as {}", command.model(), aiPolicy.defaultModel());
    }
    var cost = aiPolicy.cost(command.model(), tokens);
    usages.addTokens(
        AiUsageId.generate().value(),
        churchId.value(),
        aiAllowance.currentMonth(),
        tokens.input(),
        tokens.output(),
        tokens.cacheRead(),
        tokens.cacheWrite(),
        cost);
    return new RecordAiUsageResult(cost, known);
  }
}
