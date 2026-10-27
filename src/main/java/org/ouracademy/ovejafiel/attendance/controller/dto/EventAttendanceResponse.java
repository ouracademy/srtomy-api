package org.ouracademy.ovejafiel.attendance.controller.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;

@Builder
@Getter
public class EventAttendanceResponse {
  final List<UUID> personIds;

  /** Visitors who gave no name, only counted. */
  final int guestCount;

  /** How and when each person was marked. */
  final List<Mark> marks;

  public record Mark(UUID personId, CheckInSource source, LocalDateTime markedAt) {}
}
