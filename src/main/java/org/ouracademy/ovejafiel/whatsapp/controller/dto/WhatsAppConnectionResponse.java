package org.ouracademy.ovejafiel.whatsapp.controller.dto;

import java.time.LocalDateTime;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.ouracademy.ovejafiel.whatsapp.query.get_connection.GetConnectionResult;

public record WhatsAppConnectionResponse(
    GetConnectionResult.State status,
    ConnectionMode mode,
    String displayNumber,
    LocalDateTime connectedAt,
    boolean inherited) {

  public static WhatsAppConnectionResponse from(GetConnectionResult result) {
    return new WhatsAppConnectionResponse(
        result.state(),
        result.mode(),
        result.displayNumber(),
        result.connectedAt(),
        result.inherited());
  }
}
