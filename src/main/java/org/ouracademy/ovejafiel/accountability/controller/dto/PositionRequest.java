package org.ouracademy.ovejafiel.accountability.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The new name of a position.
 *
 * @param name Name of the position (cargo), such as Pastor or Usher.
 */
public record PositionRequest(@NotBlank @Size(max = 255) String name) {}
