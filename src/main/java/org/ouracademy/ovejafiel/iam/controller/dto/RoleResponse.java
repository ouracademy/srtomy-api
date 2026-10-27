package org.ouracademy.ovejafiel.iam.controller.dto;

import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.iam.query.RoleResult;

public record RoleResponse(
    UUID id,
    String name,
    String description,
    List<String> permissions,
    boolean predefined,
    boolean assignable) {

  public static RoleResponse from(RoleResult result) {
    return new RoleResponse(
        result.id().value(),
        result.name(),
        result.description(),
        result.permissions(),
        result.predefined(),
        result.assignable());
  }
}
