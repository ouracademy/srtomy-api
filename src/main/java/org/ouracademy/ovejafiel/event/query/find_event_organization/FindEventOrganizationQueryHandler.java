package org.ouracademy.ovejafiel.event.query.find_event_organization;

import java.util.Optional;

public interface FindEventOrganizationQueryHandler {
  Optional<EventOrganizationResult> handle(FindEventOrganizationQuery query);
}
