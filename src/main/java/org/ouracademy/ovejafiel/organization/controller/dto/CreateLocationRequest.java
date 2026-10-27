package org.ouracademy.ovejafiel.organization.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** A location under the church {@code parentId}. */
public class CreateLocationRequest extends OrganizationRequest {

  @NotNull
  @Override
  public UUID getParentId() {
    return super.getParentId();
  }

  @Override
  public void setParentId(UUID parentId) {
    super.setParentId(parentId);
  }
}
