package org.ouracademy.srtomy.event.query.list_event_registrations;

import java.util.List;

public interface ListEventRegistrationsQueryHandler {

  /** Oldest first. */
  List<EventRegistrationResult> handle(ListEventRegistrationsQuery query);
}
