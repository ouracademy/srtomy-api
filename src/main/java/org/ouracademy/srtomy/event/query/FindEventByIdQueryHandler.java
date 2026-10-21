package org.ouracademy.srtomy.event.query;

import java.util.Optional;

public interface FindEventByIdQueryHandler {
  /** Empty when no single event has the given id (including when it is a recurrent event's id). */
  Optional<FindEventResult> handle(FindEventByIdQuery query);
}
