package org.ouracademy.srtomy.event.query;

import java.util.List;

public interface SearchEventsQueryHandler {
  List<FindEventResult> handle(SearchEventsQuery query);
}
