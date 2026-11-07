package org.ouracademy.ovejafiel.event.query.list_event_notices;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.repository.EventNoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ListEventNoticesQueryHandlerImpl implements ListEventNoticesQueryHandler {

  private final EventNoticeRepository eventNoticeRepository;

  @Override
  @Transactional(readOnly = true)
  public List<NoticeResult> handle(ListEventNoticesQuery query) {
    return eventNoticeRepository.findTop20ByEventIdOrderByCreatedAtDesc(query.eventId()).stream()
        .map(NoticeResult::from)
        .toList();
  }
}
