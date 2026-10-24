package org.ouracademy.srtomy.person.command.create_person;

import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.VISITOR;

import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.command.create_accountabilities.CreateAccountabilitiesCommand;
import org.ouracademy.srtomy.accountability.command.create_accountabilities.CreateAccountabilitiesCommandHandler;
import org.ouracademy.srtomy.accountability.command.create_accountability.CreateAccountabilityCommand;
import org.ouracademy.srtomy.accountability.command.sync_person_positions.SyncPersonPositionsCommand;
import org.ouracademy.srtomy.accountability.command.sync_person_positions.SyncPersonPositionsCommandHandler;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.person.domain.Person;
import org.ouracademy.srtomy.person.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreatePersonCommandHandlerImpl implements CreatePersonCommandHandler {

  private final PersonRepository personRepository;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  private final SyncPersonPositionsCommandHandler syncPersonPositionsCommandHandler;
  private final CreateAccountabilitiesCommandHandler createAccountabilitiesCommandHandler;

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
        new SyncPersonPositionsCommand(personId, organizationId, churchId, command.positionIds()));
    createAccountabilitiesCommandHandler.handle(
        new CreateAccountabilitiesCommand(
            List.of(
                new CreateAccountabilityCommand(personId, organizationId, VISITOR),
                new CreateAccountabilityCommand(personId, organizationId, IS_PART_OF))));

    return new CreatePersonResult(personId);
  }
}
