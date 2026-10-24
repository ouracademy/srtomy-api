package org.ouracademy.srtomy.event.query.list_events_shared_with;

import java.util.List;

public interface ListEventsSharedWithQueryHandler {

  /** Soonest first; empty unless the user has a verified email. */
  List<SharedWithMeResult> handle(ListEventsSharedWithQuery query);
}
