package org.ouracademy.srtomy.accountability.controller.dto;

import java.util.UUID;
import org.ouracademy.srtomy.accountability.query.PositionSummaryResult;

/** A position with how many people hold it, so the UI can warn before changing its access. */
public record PositionSummaryResponse(UUID id, String name, UUID accessRoleId, long holders) {

  public static PositionSummaryResponse from(PositionSummaryResult result) {
    var position = PositionResponse.from(result.position());
    return new PositionSummaryResponse(
        position.id(), position.name(), position.accessRoleId(), result.holders());
  }
}
