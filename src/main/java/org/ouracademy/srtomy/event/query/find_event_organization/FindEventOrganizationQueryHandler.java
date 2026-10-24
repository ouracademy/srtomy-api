package org.ouracademy.srtomy.event.query.find_event_organization;

import java.util.Optional;

public interface FindEventOrganizationQueryHandler {
  Optional<EventOrganizationResult> handle(FindEventOrganizationQuery query);
}
