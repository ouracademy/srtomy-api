package org.ouracademy.srtomy.organization.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Data;
import org.ouracademy.srtomy.organization.domain.MemberVisibility;

@Data
public class OrganizationRequest {
  @NotBlank String name;

  String description;

  String address;

  String phoneNumber;

  @Valid SocialLinksRequest socialLinks;

  /** When the node meets (free text). */
  String schedule;

  /** Who sees the list of people; omitted keeps the current one (public for a new node). */
  MemberVisibility memberVisibility;

  /** Whether the public page shows the org chart; omitted keeps the current value. */
  Boolean showOrgChart;

  UUID parentId;
}
