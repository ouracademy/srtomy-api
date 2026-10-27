package org.ouracademy.ovejafiel.attendance.command.add_visitor;

import java.util.NoSuchElementException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.attendance.command.mark_person_present.MarkPersonPresentCommand;
import org.ouracademy.ovejafiel.attendance.command.mark_person_present.MarkPersonPresentCommandHandler;
import org.ouracademy.ovejafiel.event.query.find_event_organization.FindEventOrganizationQuery;
import org.ouracademy.ovejafiel.event.query.find_event_organization.FindEventOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonCommand;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonCommandHandler;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.user.command.link_user_to_person.LinkUserToPersonCommand;
import org.ouracademy.ovejafiel.user.command.link_user_to_person.LinkUserToPersonCommandHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class AddVisitorCommandHandlerImpl implements AddVisitorCommandHandler {

  private final FindEventOrganizationQueryHandler findEventOrganizationQueryHandler;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  private final CreatePersonCommandHandler createPersonCommandHandler;
  private final MarkPersonPresentCommandHandler markPersonPresentCommandHandler;
  private final LinkUserToPersonCommandHandler linkUserToPersonCommandHandler;

  @Override
  @Transactional
  public AddVisitorResult handle(AddVisitorCommand command) {
    var organizationId =
        findEventOrganizationQueryHandler
            .handle(new FindEventOrganizationQuery(command.eventId()))
            .orElseThrow(
                () -> new NoSuchElementException("Event not found: " + command.eventId().value()))
            .organizationId();
    var personId =
        createPersonCommandHandler
            .handle(
                new CreatePersonCommand(
                    command.subject(),
                    command.info(),
                    organizationId,
                    Set.of(),
                    MemberState.VISITOR))
            .id();
    markPersonPresentCommandHandler.handle(
        new MarkPersonPresentCommand(
            command.eventId(), personId, command.source(), command.markedBy()));
    if (command.linkTo() != null) {
      var churchId =
          findChurchOfOrganizationQueryHandler
              .handle(new FindChurchOfOrganizationQuery(organizationId))
              .orElseThrow(
                  () ->
                      new NoSuchElementException("Church not found for: " + organizationId.value()))
              .churchId();
      linkUserToPersonCommandHandler.handle(
          new LinkUserToPersonCommand(command.linkTo(), personId, churchId));
    }
    return new AddVisitorResult(personId);
  }
}
