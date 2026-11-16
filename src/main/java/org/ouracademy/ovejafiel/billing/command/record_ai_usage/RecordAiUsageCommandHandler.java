package org.ouracademy.ovejafiel.billing.command.record_ai_usage;

public interface RecordAiUsageCommandHandler {
  RecordAiUsageResult handle(RecordAiUsageCommand command);
}
