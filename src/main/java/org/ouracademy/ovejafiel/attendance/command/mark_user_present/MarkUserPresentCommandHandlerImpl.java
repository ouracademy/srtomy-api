package org.ouracademy.ovejafiel.attendance.command.mark_user_present;

import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.attendance.command.mark_person_present.MarkPersonPresentCommand;
import org.ouracademy.ovejafiel.attendance.command.mark_person_present.MarkPersonPresentCommandHandler;
import org.ouracademy.ovejafiel.event.query.find_event_organization.FindEventOrganizationQuery;
import org.ouracademy.ovejafiel.event.query.find_event_organization.FindEventOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonQuery;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class MarkUserPresentCommandHandlerImpl implements MarkUserPresentCommandHandler {

  private final FindEventOrganizationQueryHandler findEventOrganizationQueryHandler;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  private final FindLinkedPersonQueryHandler findLinkedPersonQueryHandler;
  private final MarkPersonPresentCommandHandler markPersonPresentCommandHandler;

  @Override
  @Transactional
  public Optional<MarkUserPresentResult> handle(MarkUserPresentCommand command) {
    var organizationId =
        findEventOrganizationQueryHandler
            .handle(new FindEventOrganizationQuery(command.eventId()))
            .orElseThrow(
                () -> new NoSuchElementException("Event not found: " + command.eventId().value()))
            .organizationId();
    var churchId =
        findChurchOfOrganizationQueryHandler
            .handle(new FindChurchOfOrganizationQuery(organizationId))
            .orElseThrow(
                () -> new NoSuchElementException("Church not found for: " + organizationId.value()))
            .churchId();
    return findLinkedPersonQueryHandler
        .handle(new FindLinkedPersonQuery(command.userId(), churchId))
        .map(
            linked -> {
              var marked =
                  markPersonPresentCommandHandler.handle(
                      new MarkPersonPresentCommand(
                          command.eventId(),
                          linked.personId(),
                          command.source(),
                          command.markedBy()));
              return new MarkUserPresentResult(linked.personId(), marked.alreadyMarked());
            });
  }
}
