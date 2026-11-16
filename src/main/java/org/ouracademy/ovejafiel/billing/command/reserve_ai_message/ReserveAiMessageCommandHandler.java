package org.ouracademy.ovejafiel.billing.command.reserve_ai_message;

public interface ReserveAiMessageCommandHandler {
  ReserveAiMessageResult handle(ReserveAiMessageCommand command);
}
