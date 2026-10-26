package org.ouracademy.ovejafiel.directory.controller.dto;

import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.directory.query.DirectoryNodeResult;
import org.ouracademy.ovejafiel.organization.domain.MemberVisibility;
import org.ouracademy.ovejafiel.organization.domain.Organization;

public record DirectoryNodeResponse(
    UUID id,
    String name,
    Organization.Type type,
    UUID parentId,
    String description,
    String schedule,
    MemberVisibility memberVisibility,
    List<String> leaders) {

  public static DirectoryNodeResponse from(DirectoryNodeResult result) {
    return new DirectoryNodeResponse(
        result.id().value(),
        result.name(),
        result.type(),
        result.parentId() == null ? null : result.parentId().value(),
        result.description(),
        result.schedule(),
        result.memberVisibility(),
        result.leaders());
  }
}
