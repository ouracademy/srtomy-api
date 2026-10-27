package org.ouracademy.ovejafiel.event.query.get_event_sharing;

import java.util.Optional;
import org.ouracademy.ovejafiel.event.query.EventSharingResult;

public interface GetEventSharingQueryHandler {
  Optional<EventSharingResult> handle(GetEventSharingQuery query);
}
