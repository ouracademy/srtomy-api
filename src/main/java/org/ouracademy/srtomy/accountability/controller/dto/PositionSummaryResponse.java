package org.ouracademy.srtomy.accountability.controller.dto;

import java.util.List;
import java.util.UUID;
import org.ouracademy.srtomy.accountability.query.PositionSummaryResult;

/**
 * A position with all the permissions it gives ({@code effectivePermissions}: roles plus loose
 * ones) and the people who hold it.
 */
public record PositionSummaryResponse(
    UUID id,
    String name,
    List<UUID> roleIds,
    List<String> permissions,
    List<String> effectivePermissions,
    List<PositionHolderResponse> holders) {

  public static PositionSummaryResponse from(PositionSummaryResult result) {
    var position = PositionResponse.from(result.position());
    return new PositionSummaryResponse(
        position.id(),
        position.name(),
        position.roleIds(),
        position.permissions(),
        result.effectivePermissions(),
        result.holders().stream().map(PositionHolderResponse::from).toList());
  }
}
