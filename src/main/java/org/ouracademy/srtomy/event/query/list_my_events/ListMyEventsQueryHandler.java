package org.ouracademy.srtomy.event.query.list_my_events;

import java.util.List;

public interface ListMyEventsQueryHandler {

  /** Most recent first; empty for an unknown user. */
  List<MyEventResult> handle(ListMyEventsQuery query);
}
