package org.ouracademy.ovejafiel.organization.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * A new church with its mandatory first location and its initial positions (cargos). {@code
 * positions} may be empty or missing; repeated names are created once.
 */
public record CreateChurchRequest(
    @NotBlank String name,
    String description,
    String address,
    String phoneNumber,
    @Valid SocialLinksRequest socialLinks,
    @NotNull @Valid LocationInfoRequest location,
    /** Positions to create with their access level. */
    @Valid List<ChurchPositionRequest> positions,
    /** The person who creates the church, as one of its people. */
    @NotNull @Valid CreatorRequest creator) {

  public CreateChurchRequest {
    positions = positions == null ? List.of() : positions;
  }

  /** What the position gives: roles and/or loose permissions; both empty is a plain label. */
  public record ChurchPositionRequest(
      @NotBlank String name, List<UUID> roleIds, List<String> permissions) {

    public ChurchPositionRequest {
      roleIds = roleIds == null ? List.of() : roleIds;
      permissions = permissions == null ? List.of() : permissions;
    }
  }

  /** {@code positionName} is created as a plain label when it is not in {@code positions}. */
  public record CreatorRequest(
      @NotBlank String firstName, @NotBlank String lastName, String positionName) {}

  public record LocationInfoRequest(
      @NotBlank String name,
      String description,
      String address,
      String phoneNumber,
      @Valid SocialLinksRequest socialLinks) {}
}
