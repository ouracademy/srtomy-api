package org.ouracademy.ovejafiel.whatsapp.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.ouracademy.ovejafiel.whatsapp.domain.DisconnectReason;
import org.ouracademy.ovejafiel.whatsapp.query.get_connection.GetConnectionResult;

public record WhatsAppConnectionResponse(
    GetConnectionResult.State status,
    ConnectionMode mode,
    String displayNumber,
    LocalDateTime connectedAt,
    boolean inherited,
    @Schema(description = "Why the number stopped working, when status is DISCONNECTED")
        DisconnectReason disconnectReason) {

  public static WhatsAppConnectionResponse from(GetConnectionResult result) {
    return new WhatsAppConnectionResponse(
        result.state(),
        result.mode(),
        result.displayNumber(),
        result.connectedAt(),
        result.inherited(),
        result.disconnectReason());
  }
}
