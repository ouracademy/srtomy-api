package org.ouracademy.srtomy.accountability.controller.dto;

import java.util.UUID;
import org.ouracademy.srtomy.accountability.query.PositionResult;

/** A position; {@code accessRoleId} is its access level, null when it is only a label. */
public record PositionResponse(UUID id, String name, UUID accessRoleId) {

  public static PositionResponse from(PositionResult result) {
    return new PositionResponse(
        result.id().value(),
        result.name(),
        result.accessRoleId() == null ? null : result.accessRoleId().value());
  }
}
