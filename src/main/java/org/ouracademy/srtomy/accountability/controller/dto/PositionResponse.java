package org.ouracademy.srtomy.accountability.controller.dto;

import java.util.UUID;
import org.ouracademy.srtomy.accountability.query.PositionResult;

public record PositionResponse(UUID id, String name) {

  public static PositionResponse from(PositionResult result) {
    return new PositionResponse(result.id().value(), result.name());
  }
}
