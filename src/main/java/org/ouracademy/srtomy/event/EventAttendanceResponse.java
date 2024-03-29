package org.ouracademy.srtomy.event;

import java.util.List;

import lombok.Builder;

@Builder
public class EventAttendanceResponse {
    final Integer numberOfVisitors;
    final String visitorsDescription;
    final List<Long> personIds;
}
