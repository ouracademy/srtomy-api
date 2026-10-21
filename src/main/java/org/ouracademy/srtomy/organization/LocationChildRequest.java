package org.ouracademy.srtomy.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LocationChildRequest extends OrganizationRequest {

  @NotNull Organization.Type type;

  @NotEmpty List<UUID> leaderIds = new ArrayList<>();

  @NotEmpty List<UUID> memberIds = new ArrayList<>();

  @NotNull
  @Override
  public UUID getParentId() {
    return super.getParentId();
  }

  @Override
  public void setParentId(UUID parentId) {
    super.setParentId(parentId);
  }

  @Schema(hidden = true)
  @AssertTrue
  public boolean isChild() {
    return type.equals(Organization.Type.GROUP) || type.equals(Organization.Type.MINISTRY);
  }
}
