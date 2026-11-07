package org.ouracademy.ovejafiel.event.query.list_event_notices;

import java.util.List;

public interface ListEventNoticesQueryHandler {
  /** The latest notices of the event, the newest first. */
  List<NoticeResult> handle(ListEventNoticesQuery query);
}
