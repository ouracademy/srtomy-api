package org.ouracademy.ovejafiel.attendance.controller.dto;

import jakarta.validation.constraints.Size;

/** Optional: the name of someone who has no person in the church yet. */
public record SelfAttendanceRequest(@Size(max = 150) String name) {}
