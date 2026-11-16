package org.ouracademy.ovejafiel.billing.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.billing.command.record_ai_usage.RecordAiUsageCommand;
import org.ouracademy.ovejafiel.billing.command.record_ai_usage.RecordAiUsageCommandHandler;
import org.ouracademy.ovejafiel.billing.command.record_ai_usage.RecordAiUsageResult;
import org.ouracademy.ovejafiel.billing.command.reserve_ai_message.ReserveAiMessageCommand;
import org.ouracademy.ovejafiel.billing.command.reserve_ai_message.ReserveAiMessageCommandHandler;
import org.ouracademy.ovejafiel.billing.command.reserve_ai_message.ReserveAiMessageResult;
import org.ouracademy.ovejafiel.billing.controller.dto.RecordAiUsageRequest;
import org.ouracademy.ovejafiel.billing.controller.dto.ReserveAiMessageRequest;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * For srtomy-agent only (its service account, see {@code InternalSecurityConfig}): it asks before
 * calling the model and tells the tokens after. A denial is a 200 with {@code allowed: false}, so
 * the agent tells the person why; any error means the agent must not call the model.
 */
@RestController
@AllArgsConstructor
@Hidden
public class AiUsageController {

  ReserveAiMessageCommandHandler reserveAiMessageCommandHandler;
  RecordAiUsageCommandHandler recordAiUsageCommandHandler;

  @PostMapping("internal/billing/ai-usage/reserve")
  public ReserveAiMessageResult reserve(@RequestBody @Valid ReserveAiMessageRequest request) {
    return reserveAiMessageCommandHandler.handle(
        new ReserveAiMessageCommand(new PartyId(request.nodeId()), request.channel()));
  }

  @PostMapping("internal/billing/ai-usage/record")
  public RecordAiUsageResult record(@RequestBody @Valid RecordAiUsageRequest request) {
    return recordAiUsageCommandHandler.handle(
        new RecordAiUsageCommand(
            new PartyId(request.nodeId()),
            request.model(),
            request.inputTokens(),
            request.outputTokens(),
            request.cacheReadTokens(),
            request.cacheWriteTokens()));
  }
}
