package org.ouracademy.ovejafiel.event.command;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventNotice;
import org.ouracademy.ovejafiel.event.domain.NoticeKind;
import org.ouracademy.ovejafiel.event.repository.EventNoticeRepository;
import org.springframework.stereotype.Component;

/** Writes the notices that show on the page of an event. */
@Component
@RequiredArgsConstructor
public class EventNotices {

  private final EventNoticeRepository eventNoticeRepository;

  public EventNotice post(Event event, NoticeKind kind, String message) {
    return eventNoticeRepository.save(new EventNotice(event, kind, message));
  }
}
