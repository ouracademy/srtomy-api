package org.ouracademy.ovejafiel.billing.command.reserve_ai_message;

import org.ouracademy.ovejafiel.billing.domain.AiChannel;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * The agent is about to call the model for someone of the church of {@code nodeId} (any node of its
 * tree): one message of the month is counted if the plan still allows it.
 */
public record ReserveAiMessageCommand(PartyId nodeId, AiChannel channel) {}
