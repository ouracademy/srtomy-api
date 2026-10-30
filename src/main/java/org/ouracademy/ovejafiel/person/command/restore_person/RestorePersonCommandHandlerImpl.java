package org.ouracademy.ovejafiel.person.command.restore_person;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.command.set_node_membership.SetNodeMembershipCommand;
import org.ouracademy.ovejafiel.accountability.command.set_node_membership.SetNodeMembershipCommandHandler;
import org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQuery;
import org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_ancestors.FindOrganizationAncestorsQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_ancestors.FindOrganizationAncestorsQueryHandler;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
import org.ouracademy.ovejafiel.shared.domain.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class RestorePersonCommandHandlerImpl implements RestorePersonCommandHandler {

  private final PersonRepository personRepository;
  private final FindOrganizationsOfMemberQueryHandler findOrganizationsOfMemberQueryHandler;
  private final SetNodeMembershipCommandHandler setNodeMembershipCommandHandler;
  private final FindOrganizationAncestorsQueryHandler findOrganizationAncestorsQueryHandler;

  @Override
  @Transactional
  public RestorePersonResult handle(RestorePersonCommand command) {
    var person =
        personRepository
            .findById(command.personId())
            .orElseThrow(
                () ->
                    new NoSuchElementException("Person not found: " + command.personId().value()));
    var alreadyThere =
        findOrganizationsOfMemberQueryHandler
            .handle(new FindOrganizationsOfMemberQuery(command.personId()))
            .organizationIds()
            .contains(command.organizationId());
    if (alreadyThere) {
      throw new ConflictException("PERSON_ALREADY_ACTIVE", "La persona ya pertenece a este grupo");
    }

    person.restore();
    setNodeMembershipCommandHandler.handle(
        new SetNodeMembershipCommand(
            command.personId(),
            command.organizationId(),
            findOrganizationAncestorsQueryHandler
                .handle(new FindOrganizationAncestorsQuery(command.organizationId()))
                .ids(),
            MemberState.VISITOR.typeId()));
    return new RestorePersonResult(command.personId());
  }
}
