package org.ouracademy.ovejafiel.event.command.post_event_notice;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.command.EventNotices;
import org.ouracademy.ovejafiel.event.domain.NoticeKind;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class PostEventNoticeCommandHandlerImpl implements PostEventNoticeCommandHandler {

  private final EventRepository eventRepository;
  private final EventNotices eventNotices;

  @Override
  @Transactional
  public PostEventNoticeResult handle(PostEventNoticeCommand command) {
    var event =
        eventRepository
            .findById(command.eventId())
            .orElseThrow(() -> new NoSuchElementException("Event not found"));
    eventNotices.post(event, NoticeKind.ANNOUNCEMENT, command.message());
    return new PostEventNoticeResult();
  }
}
