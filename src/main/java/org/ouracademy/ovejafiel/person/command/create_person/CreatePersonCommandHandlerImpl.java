package org.ouracademy.ovejafiel.person.command.create_person;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.command.set_node_membership.SetNodeMembershipCommand;
import org.ouracademy.ovejafiel.accountability.command.set_node_membership.SetNodeMembershipCommandHandler;
import org.ouracademy.ovejafiel.accountability.command.sync_person_positions.SyncPersonPositionsCommand;
import org.ouracademy.ovejafiel.accountability.command.sync_person_positions.SyncPersonPositionsCommandHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_ancestors.FindOrganizationAncestorsQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_ancestors.FindOrganizationAncestorsQueryHandler;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreatePersonCommandHandlerImpl implements CreatePersonCommandHandler {

  private final PersonRepository personRepository;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  private final SyncPersonPositionsCommandHandler syncPersonPositionsCommandHandler;
  private final SetNodeMembershipCommandHandler setNodeMembershipCommandHandler;
  private final FindOrganizationAncestorsQueryHandler findOrganizationAncestorsQueryHandler;

  @Override
  @Transactional
  public CreatePersonResult handle(CreatePersonCommand command) {
    var organizationId = command.organizationId();
    var churchId =
        findChurchOfOrganizationQueryHandler
            .handle(new FindChurchOfOrganizationQuery(organizationId))
            .orElseThrow(
                () ->
                    new NoSuchElementException("Organization not found: " + organizationId.value()))
            .churchId();

    var person = new Person();
    command.info().applyTo(person);
    person = personRepository.save(person);
    var personId = person.getId();

    syncPersonPositionsCommandHandler.handle(
        new SyncPersonPositionsCommand(
            command.subject(), personId, organizationId, churchId, command.positionIds()));
    // Being in a node means being known by the nodes above it: the church knows all its people.
    setNodeMembershipCommandHandler.handle(
        new SetNodeMembershipCommand(
            personId,
            organizationId,
            findOrganizationAncestorsQueryHandler
                .handle(new FindOrganizationAncestorsQuery(organizationId))
                .ids(),
            stateOf(command).typeId()));

    return new CreatePersonResult(personId);
  }

  /** Whoever holds a position is a member; otherwise the state asked for, or visitor. */
  private static MemberState stateOf(CreatePersonCommand command) {
    if (!command.positionIds().isEmpty()) {
      return MemberState.MEMBER;
    }
    return command.memberState() == null ? MemberState.VISITOR : command.memberState();
  }
}
