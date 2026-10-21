package org.ouracademy.srtomy.event.query.find_event_by_id;

import java.util.Optional;
import org.ouracademy.srtomy.event.query.FindEventResult;

public interface FindEventByIdQueryHandler {
  /** Empty when no single event has the given id (including when it is a recurrent event's id). */
  Optional<FindEventResult> handle(FindEventByIdQuery query);
}
