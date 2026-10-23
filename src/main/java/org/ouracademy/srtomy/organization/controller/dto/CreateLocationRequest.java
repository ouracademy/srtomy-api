package org.ouracademy.srtomy.organization.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

public class CreateLocationRequest extends OrganizationRequest {

  @Getter @Setter @NotEmpty SortedSet<String> roles = new TreeSet<>();

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
