package org.ouracademy.ovejafiel.iam.controller.dto;

import java.util.UUID;
import org.ouracademy.ovejafiel.iam.query.list_access.AccessEntryResult;

/**
 * {@code bindingId} is only set for direct accesses (Owner); {@code source} says where the access
 * comes from and {@code positionName} is the position when it comes from one.
 */
public record AccessEntryResponse(
    UUID bindingId,
    UUID userId,
    String email,
    UUID roleId,
    String roleName,
    UUID organizationId,
    String organizationName,
    boolean inherited,
    AccessEntryResult.Source source,
    String positionName,
    int permissionCount) {

  public static AccessEntryResponse from(AccessEntryResult result) {
    return new AccessEntryResponse(
        result.bindingId() == null ? null : result.bindingId().value(),
        result.userId().value(),
        result.email(),
        result.roleId() == null ? null : result.roleId().value(),
        result.roleName(),
        result.organizationId().value(),
        result.organizationName(),
        result.inherited(),
        result.source(),
        result.positionName(),
        result.permissionCount());
  }
}
