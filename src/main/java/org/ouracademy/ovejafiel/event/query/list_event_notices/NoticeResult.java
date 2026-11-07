package org.ouracademy.ovejafiel.event.query.list_event_notices;

import java.time.LocalDateTime;
import org.ouracademy.ovejafiel.event.domain.EventNotice;
import org.ouracademy.ovejafiel.event.domain.NoticeKind;

/** A notice of an event as it is shown. */
public record NoticeResult(NoticeKind kind, String message, LocalDateTime createdAt) {

  public static NoticeResult from(EventNotice notice) {
    return new NoticeResult(notice.getKind(), notice.getMessage(), notice.getCreatedAt());
  }
}
