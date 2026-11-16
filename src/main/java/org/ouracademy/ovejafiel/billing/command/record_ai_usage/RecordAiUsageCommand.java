package org.ouracademy.ovejafiel.billing.command.record_ai_usage;

import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * The tokens of one answer of the assistant for the church of {@code nodeId}, as the AI SDK reports
 * them ({@code usage}). {@code model} is the id the agent called, to price the tokens.
 */
public record RecordAiUsageCommand(
    PartyId nodeId,
    String model,
    long inputTokens,
    long outputTokens,
    long cacheReadTokens,
    long cacheWriteTokens) {}
