package org.ouracademy.srtomy.accountability.controller.dto;

import java.util.List;
import java.util.UUID;
import org.ouracademy.srtomy.accountability.query.PositionResult;
import org.ouracademy.srtomy.iam.domain.IamRoleId;

/**
 * A position. {@code roleIds} and the loose {@code permissions} are what it gives; with neither it
 * is only a label.
 */
public record PositionResponse(UUID id, String name, List<UUID> roleIds, List<String> permissions) {

  public static PositionResponse from(PositionResult result) {
    return new PositionResponse(
        result.id().value(),
        result.name(),
        result.roleIds().stream().map(IamRoleId::value).sorted().toList(),
        result.permissions().stream().sorted().toList());
  }
}
