package org.ouracademy.ovejafiel.attendance.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Who attended. {@code personIds} is required: a missing or null list is rejected, so a client
 * cannot clear the attendance by mistake (an empty list is how it is cleared).
 *
 * @param personIds Ids of everyone who attended. An empty list clears the attendance.
 */
public record AttendanceRequest(@NotNull List<UUID> personIds) {}
