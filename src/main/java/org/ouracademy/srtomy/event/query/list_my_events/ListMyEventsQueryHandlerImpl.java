package org.ouracademy.srtomy.event.query.list_my_events;

import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ListMyEventsQueryHandlerImpl implements ListMyEventsQueryHandler {

  private final FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;
  private final EventRepository eventRepository;

  @Override
  @Transactional(readOnly = true)
  public List<MyEventResult> handle(ListMyEventsQuery query) {
    return findUserBySubjectQueryHandler
        .handle(new FindUserBySubjectQuery(query.subject()))
        .map(
            user ->
                eventRepository
                    .findOfUser(
                        user.id().value(),
                        // An email nobody verified proves nothing about who owns it.
                        user.emailVerified() ? user.email() : null)
                    .stream()
                    .map(
                        row ->
                            new MyEventResult(
                                row.getTitle(),
                                row.getStart().atZone(ZoneOffset.UTC),
                                Boolean.TRUE.equals(row.getIsACourse()),
                                row.getOrganizationName(),
                                row.getRegistered(),
                                row.getAttended(),
                                row.getSlug()))
                    .toList())
        .orElse(List.of());
  }
}
