package org.ouracademy.ovejafiel.event.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code startTime} is when the copy starts (ISO with offset); {@code name} defaults to the same.
 */
public record CopyEventRequest(@NotBlank String startTime, String name) {}
