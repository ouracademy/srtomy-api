package org.ouracademy.ovejafiel.event.query.find_events_in_range;

import java.util.List;
import org.ouracademy.ovejafiel.event.query.FindEventResult;

public interface FindEventsInRangeQueryHandler {
  List<FindEventResult> handle(FindEventsInRangeQuery query);
}
