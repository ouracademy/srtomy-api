package org.ouracademy.srtomy.event;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EventAttendanceResponse {
  final List<UUID> personIds;
}
