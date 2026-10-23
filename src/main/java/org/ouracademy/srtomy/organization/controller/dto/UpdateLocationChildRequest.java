package org.ouracademy.srtomy.organization.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.ouracademy.srtomy.organization.domain.Organization;

@Setter
@Getter
public class UpdateLocationChildRequest extends OrganizationRequest {

  @NotNull Organization.Type type;

  @NotEmpty List<UUID> leaderIds = new ArrayList<>();

  @NotEmpty List<UUID> memberIds = new ArrayList<>();

  @Schema(hidden = true)
  @AssertTrue
  public boolean isChild() {
    return type.equals(Organization.Type.GROUP) || type.equals(Organization.Type.MINISTRY);
  }
}
