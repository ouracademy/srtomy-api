package org.ouracademy.srtomy.user.controller.dto;

import java.util.List;
import java.util.UUID;
import org.ouracademy.srtomy.iam.query.find_accessible_organizations.AccessibleOrganizationResult;
import org.ouracademy.srtomy.organization.domain.Organization;

/** A node of the user's trees; the client builds the tree from {@code parentId}. */
public record MyOrganizationResponse(
    UUID id,
    String name,
    Organization.Type type,
    UUID parentId,
    boolean accessible,
    List<String> roles) {

  public static MyOrganizationResponse from(AccessibleOrganizationResult result) {
    return new MyOrganizationResponse(
        result.id().value(),
        result.name(),
        result.type(),
        result.parentId() == null ? null : result.parentId().value(),
        result.accessible(),
        result.roles());
  }
}
