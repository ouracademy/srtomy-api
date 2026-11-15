package org.ouracademy.ovejafiel.organization.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * A new church with its mandatory first location and its initial positions (cargos). {@code
 * positions} may be empty or missing; repeated names are created once.
 *
 * @param name Name of the church.
 * @param description What the church is about. Optional.
 * @param address Street address of the church. Optional.
 * @param phoneNumber Contact phone number. Optional.
 * @param socialLinks Its website and social networks. Optional.
 * @param location The first location (sede) of the church.
 * @param positions Positions to create with their access level. Optional.
 * @param creator The person who creates the church, as one of its people.
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

  /**
   * What the position gives: roles and/or loose permissions; both empty is a plain label.
   *
   * @param name Name of the position (cargo), such as Pastor.
   * @param roleIds Ids of access levels, from the roles of the church. Optional.
   * @param permissions Loose permission values, such as {@code members.read}; the full list is in
   *     {@code GET /v1/iam/permissions}. Optional.
   */
  public record ChurchPositionRequest(
      @NotBlank String name, List<UUID> roleIds, List<String> permissions) {

    public ChurchPositionRequest {
      roleIds = roleIds == null ? List.of() : roleIds;
      permissions = permissions == null ? List.of() : permissions;
    }
  }

  /**
   * {@code positionName} is created as a plain label when it is not in {@code positions}.
   *
   * @param firstName First names of who creates the church.
   * @param lastName Last names of who creates the church.
   * @param positionName Name of the position they hold. Optional.
   */
  public record CreatorRequest(
      @NotBlank String firstName, @NotBlank String lastName, String positionName) {}

  /**
   * The first location (sede) of a new church.
   *
   * @param name Name of the location.
   * @param description What the location is about. Optional.
   * @param address Street address of the location. Optional.
   * @param phoneNumber Contact phone number. Optional.
   * @param socialLinks Its website and social networks. Optional.
   */
  public record LocationInfoRequest(
      @NotBlank String name,
      String description,
      String address,
      String phoneNumber,
      @Valid SocialLinksRequest socialLinks) {}
}
