package org.ouracademy.ovejafiel.attendance.controller.dto;

import jakarta.validation.constraints.Min;

/**
 * How many visitors without a name came.
 *
 * @param guestCount How many visitors without a name came; 0 or more.
 */
public record GuestsRequest(@Min(0) int guestCount) {}
