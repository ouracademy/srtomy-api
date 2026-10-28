package org.ouracademy.ovejafiel.event.query.get_event_registration;

import java.util.Optional;

/** How the event takes sign-ups, for its organizers; empty when the event does not exist. */
public interface GetEventRegistrationQueryHandler {
  Optional<GetEventRegistrationResult> handle(GetEventRegistrationQuery query);
}
