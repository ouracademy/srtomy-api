package org.ouracademy.ovejafiel.billing.command.record_ai_usage;

import java.math.BigDecimal;

/**
 * @param costUsd estimated cost of this answer
 * @param knownModel false when the model had no price and the default one was used
 */
public record RecordAiUsageResult(BigDecimal costUsd, boolean knownModel) {}
