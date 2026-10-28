package org.ouracademy.ovejafiel.attendance.controller.dto;

import jakarta.validation.constraints.Min;

/** How many visitors without a name came. */
public record GuestsRequest(@Min(0) int guestCount) {}
