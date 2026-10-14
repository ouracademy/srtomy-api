package org.ouracademy.srtomy.organization;

import jakarta.validation.constraints.NotNull;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.Getter;
import lombok.Setter;

public class CreateLocationRequest extends OrganizationRequest {

  @Getter @Setter SortedSet<String> roles = new TreeSet<>();

  @NotNull
  @Override
  public Long getParentId() {
    return super.getParentId();
  }

  @Override
  public void setParentId(Long parentId) {
    super.setParentId(parentId);
  }
}
