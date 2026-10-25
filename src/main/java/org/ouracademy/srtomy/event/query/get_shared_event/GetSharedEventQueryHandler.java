package org.ouracademy.srtomy.event.query.get_shared_event;

import java.util.Optional;

/**
 * The shared page of an event. Empty (404) unless the visitor can open it: the link is open to
 * anyone, or to the church and the visitor belongs to it, or they were shared the event, or they
 * can read it through the church tree.
 */
public interface GetSharedEventQueryHandler {
  Optional<GetSharedEventResult> handle(GetSharedEventQuery query);
}
