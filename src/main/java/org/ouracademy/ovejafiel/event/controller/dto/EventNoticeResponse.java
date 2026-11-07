package org.ouracademy.ovejafiel.event.controller.dto;

import java.time.LocalDateTime;
import org.ouracademy.ovejafiel.event.domain.NoticeKind;
import org.ouracademy.ovejafiel.event.query.list_event_notices.NoticeResult;

/** A notice of an event: what happened and when, shown on its page. */
public record EventNoticeResponse(NoticeKind kind, String message, LocalDateTime createdAt) {

  public static EventNoticeResponse from(NoticeResult result) {
    return new EventNoticeResponse(result.kind(), result.message(), result.createdAt());
  }
}
