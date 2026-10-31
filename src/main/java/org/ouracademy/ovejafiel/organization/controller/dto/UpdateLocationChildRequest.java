package org.ouracademy.ovejafiel.organization.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.ouracademy.ovejafiel.organization.domain.Organization;

@Setter
@Getter
public class UpdateLocationChildRequest extends OrganizationRequest {

  @NotNull Organization.Type type;

  @Schema(hidden = true)
  @AssertTrue
  public boolean isChild() {
    return type == Organization.Type.GROUP || type == Organization.Type.MINISTRY;
  }
}
