package org.ouracademy.srtomy.person.command.create_person;

import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.VISITOR;

import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.command.create_accountabilities.CreateAccountabilitiesCommand;
import org.ouracademy.srtomy.accountability.command.create_accountabilities.CreateAccountabilitiesCommandHandler;
import org.ouracademy.srtomy.accountability.command.create_accountability.CreateAccountabilityCommand;
import org.ouracademy.srtomy.accountability.command.sync_person_roles.SyncPersonRolesCommand;
import org.ouracademy.srtomy.accountability.command.sync_person_roles.SyncPersonRolesCommandHandler;
import org.ouracademy.srtomy.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.srtomy.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.srtomy.person.domain.Person;
import org.ouracademy.srtomy.person.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreatePersonCommandHandlerImpl implements CreatePersonCommandHandler {

  private final PersonRepository personRepository;
  private final FindOrganizationByIdQueryHandler findOrganizationByIdQueryHandler;
  private final SyncPersonRolesCommandHandler syncPersonRolesCommandHandler;
  private final CreateAccountabilitiesCommandHandler createAccountabilitiesCommandHandler;

  @Override
  @Transactional
  public CreatePersonResult handle(CreatePersonCommand command) {
    var organizationId = command.organizationId();
    findOrganizationByIdQueryHandler
        .handle(new FindOrganizationByIdQuery(organizationId))
        .orElseThrow(
            () -> new NoSuchElementException("Organization not found: " + organizationId.value()));

    var person = new Person();
    command.info().applyTo(person);
    person = personRepository.save(person);
    var personId = person.getId();

    syncPersonRolesCommandHandler.handle(
        new SyncPersonRolesCommand(personId, organizationId, command.roles()));
    createAccountabilitiesCommandHandler.handle(
        new CreateAccountabilitiesCommand(
            List.of(
                new CreateAccountabilityCommand(personId, organizationId, VISITOR),
                new CreateAccountabilityCommand(personId, organizationId, IS_PART_OF))));

    return new CreatePersonResult(personId);
  }
}
