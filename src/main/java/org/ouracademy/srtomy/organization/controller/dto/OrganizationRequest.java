package org.ouracademy.srtomy.organization.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Data;

@Data
public class OrganizationRequest {
  @NotBlank String name;

  String description;

  String address;

  String phoneNumber;

  @Valid SocialLinksRequest socialLinks;

  UUID parentId;
}
