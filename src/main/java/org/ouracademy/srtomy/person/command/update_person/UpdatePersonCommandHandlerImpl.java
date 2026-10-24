package org.ouracademy.srtomy.person.command.update_person;

import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.command.sync_person_positions.SyncPersonPositionsCommand;
import org.ouracademy.srtomy.accountability.command.sync_person_positions.SyncPersonPositionsCommandHandler;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.person.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UpdatePersonCommandHandlerImpl implements UpdatePersonCommandHandler {

  private final PersonRepository personRepository;
  private final SyncPersonPositionsCommandHandler syncPersonPositionsCommandHandler;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;

  @Override
  @Transactional
  public Optional<UpdatePersonResult> handle(UpdatePersonCommand command) {
    return personRepository
        .findById(command.personId())
        .map(
            person -> {
              var churchId =
                  findChurchOfOrganizationQueryHandler
                      .handle(new FindChurchOfOrganizationQuery(command.organizationId()))
                      .orElseThrow(
                          () ->
                              new NoSuchElementException(
                                  "Organization not found: " + command.organizationId().value()))
                      .churchId();
              command.info().applyTo(person);
              syncPersonPositionsCommandHandler.handle(
                  new SyncPersonPositionsCommand(
                      person.getId(), command.organizationId(), churchId, command.positionIds()));
              return new UpdatePersonResult(personRepository.save(person).getId());
            });
  }
}
