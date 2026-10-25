package org.ouracademy.srtomy.event.query.get_event_sharing;

import java.util.Optional;
import org.ouracademy.srtomy.event.query.EventSharingResult;

public interface GetEventSharingQueryHandler {
  Optional<EventSharingResult> handle(GetEventSharingQuery query);
}
