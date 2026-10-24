package org.ouracademy.srtomy.organization.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
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

  /** Position names to create; may be empty. Repeated names are created once. */
  @NotNull List<String> positions = new ArrayList<>();

  @Data
  public static class LocationInfoRequest {
    @NotBlank String name;

    String description;

    String address;

    String phoneNumber;

    @Valid SocialLinksRequest socialLinks;
  }
}
