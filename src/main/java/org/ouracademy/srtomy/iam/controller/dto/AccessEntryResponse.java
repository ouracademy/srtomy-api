package org.ouracademy.srtomy.iam.controller.dto;

import java.util.UUID;
import org.ouracademy.srtomy.iam.query.list_access.AccessEntryResult;

public record AccessEntryResponse(
    UUID bindingId,
    UUID userId,
    String email,
    UUID roleId,
    String roleName,
    UUID organizationId,
    String organizationName,
    boolean inherited) {

  public static AccessEntryResponse from(AccessEntryResult result) {
    return new AccessEntryResponse(
        result.bindingId().value(),
        result.userId().value(),
        result.email(),
        result.roleId().value(),
        result.roleName(),
        result.organizationId().value(),
        result.organizationName(),
        result.inherited());
  }
}
