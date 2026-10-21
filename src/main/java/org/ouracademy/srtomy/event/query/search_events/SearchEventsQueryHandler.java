package org.ouracademy.srtomy.event.query.search_events;

import java.util.List;
import org.ouracademy.srtomy.event.query.FindEventResult;

public interface SearchEventsQueryHandler {
  List<FindEventResult> handle(SearchEventsQuery query);
}
