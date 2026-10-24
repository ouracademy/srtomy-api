package org.ouracademy.srtomy.organization.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.domain.SocialLinks;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;

public record OrganizationResponse(
    UUID id,
    String name,
    String description,
    String phoneNumber,
    String address,
    Organization.Type type,
    UUID parentId,
    SocialLinks socialLinks,
    LocalDateTime createDate,
    LocalDateTime lastModifiedDate) {

  public static OrganizationResponse from(FindOrganizationResult result) {
    return new OrganizationResponse(
        result.id().value(),
        result.name(),
        result.description(),
        result.phoneNumber(),
        result.address(),
        result.type(),
        result.parentId() == null ? null : result.parentId().value(),
        result.socialLinks(),
        result.createDate(),
        result.lastModifiedDate());
  }
}
