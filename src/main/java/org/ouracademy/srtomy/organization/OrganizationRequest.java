package org.ouracademy.srtomy.organization;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Data;

@Data
public class OrganizationRequest {
  @NotBlank String name;

  String description;

  String address;

  String phoneNumber;

  UUID parentId;

  // TODO: remove this and use current user (principal)
  String userId;
}
