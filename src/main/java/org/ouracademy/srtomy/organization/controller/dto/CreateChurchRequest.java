package org.ouracademy.srtomy.organization.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;

/** A new church with its mandatory first location and its initial positions (cargos). */
@Data
public class CreateChurchRequest {
  @NotBlank String name;

  String description;

  String address;

  String phoneNumber;

  @Valid SocialLinksRequest socialLinks;

  @NotNull @Valid LocationInfoRequest location;

  /** Positions to create with their access level; may be empty. Repeated names are created once. */
  @NotNull @Valid List<ChurchPositionRequest> positions = new ArrayList<>();

  /** The person who creates the church, as one of its people. */
  @NotNull @Valid CreatorRequest creator;

  @Data
  public static class ChurchPositionRequest {
    @NotBlank String name;

    /** What the position gives: roles and/or loose permissions; both empty is a plain label. */
    List<UUID> roleIds = new ArrayList<>();

    List<String> permissions = new ArrayList<>();
  }

  @Data
  public static class CreatorRequest {
    @NotBlank String firstName;

    @NotBlank String lastName;

    /** The creator's position; created as a plain label when it is not in {@code positions}. */
    String positionName;
  }

  @Data
  public static class LocationInfoRequest {
    @NotBlank String name;

    String description;

    String address;

    String phoneNumber;

    @Valid SocialLinksRequest socialLinks;
  }
}
