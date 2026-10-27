package org.ouracademy.ovejafiel.event.query.list_my_events;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
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
                                row.getSlug(),
                                row.getRegistrationId() == null
                                    ? null
                                    : new EventRegistrationId(
                                        UUID.fromString(row.getRegistrationId())),
                                row.getTicketName(),
                                row.getRegistrationStatus() == null
                                    ? null
                                    : RegistrationStatus.valueOf(row.getRegistrationStatus()),
                                row.getCancelCode(),
                                row.getTicketCode()))
                    .toList())
        .orElse(List.of());
  }
}
