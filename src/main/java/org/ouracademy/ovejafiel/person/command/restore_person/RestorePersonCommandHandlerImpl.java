package org.ouracademy.ovejafiel.person.command.restore_person;

import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;

import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.command.create_accountabilities.CreateAccountabilitiesCommand;
import org.ouracademy.ovejafiel.accountability.command.create_accountabilities.CreateAccountabilitiesCommandHandler;
import org.ouracademy.ovejafiel.accountability.command.create_accountability.CreateAccountabilityCommand;
import org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQuery;
import org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQueryHandler;
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
  private final CreateAccountabilitiesCommandHandler createAccountabilitiesCommandHandler;

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
    createAccountabilitiesCommandHandler.handle(
        new CreateAccountabilitiesCommand(
            List.of(
                new CreateAccountabilityCommand(
                    command.personId(), command.organizationId(), MemberState.VISITOR.typeId()),
                new CreateAccountabilityCommand(
                    command.personId(), command.organizationId(), IS_PART_OF))));
    return new RestorePersonResult(command.personId());
  }
}
