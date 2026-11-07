package org.ouracademy.ovejafiel.event.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** An announcement for the page of the event. */
public record EventNoticeRequest(@NotBlank @Size(max = 1000) String message) {}
