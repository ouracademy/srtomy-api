package org.ouracademy.ovejafiel.outbox.query.find_outbox_statuses;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.outbox.repository.OutboxMessageRepository;
import org.ouracademy.ovejafiel.outbox.repository.OutboxMessageStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindOutboxStatusesQueryHandlerImpl implements FindOutboxStatusesQueryHandler {

  private final OutboxMessageRepository outboxMessageRepository;

  @Override
  @Transactional(readOnly = true)
  public FindOutboxStatusesResult handle(FindOutboxStatusesQuery query) {
    var ids = query.ids().stream().filter(java.util.Objects::nonNull).toList();
    if (ids.isEmpty()) {
      return new FindOutboxStatusesResult(Map.of());
    }
    return new FindOutboxStatusesResult(
        outboxMessageRepository.findStatusesByIdIn(ids).stream()
            .collect(Collectors.toMap(OutboxMessageStatus::id, OutboxMessageStatus::status)));
  }
}
