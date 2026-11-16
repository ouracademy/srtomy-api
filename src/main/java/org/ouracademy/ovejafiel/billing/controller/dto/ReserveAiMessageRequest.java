package org.ouracademy.ovejafiel.billing.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.ouracademy.ovejafiel.billing.domain.AiChannel;

/**
 * @param nodeId any node of the church the conversation belongs to
 * @param channel where the person wrote: PORTAL or WHATSAPP
 */
public record ReserveAiMessageRequest(@NotNull UUID nodeId, @NotNull AiChannel channel) {}
