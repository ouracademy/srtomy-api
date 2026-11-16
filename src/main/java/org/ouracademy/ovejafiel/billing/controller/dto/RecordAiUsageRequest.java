package org.ouracademy.ovejafiel.billing.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

/**
 * The {@code usage} of one answer, as the AI SDK reports it.
 *
 * @param nodeId any node of the church the conversation belongs to
 * @param model the model id that answered, e.g. claude-sonnet-5-5
 */
public record RecordAiUsageRequest(
    @NotNull UUID nodeId,
    String model,
    @PositiveOrZero long inputTokens,
    @PositiveOrZero long outputTokens,
    @PositiveOrZero long cacheReadTokens,
    @PositiveOrZero long cacheWriteTokens) {}
