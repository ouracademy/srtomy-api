package org.ouracademy.ovejafiel.event.query.search_events;

import java.util.List;
import org.ouracademy.ovejafiel.event.query.FindEventResult;

public interface SearchEventsQueryHandler {
  List<FindEventResult> handle(SearchEventsQuery query);
}
