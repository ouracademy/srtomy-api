package org.ouracademy.srtomy.person.command.update_person;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.command.sync_person_roles.SyncPersonRolesCommand;
import org.ouracademy.srtomy.accountability.command.sync_person_roles.SyncPersonRolesCommandHandler;
import org.ouracademy.srtomy.person.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UpdatePersonCommandHandlerImpl implements UpdatePersonCommandHandler {

  private final PersonRepository personRepository;
  private final SyncPersonRolesCommandHandler syncPersonRolesCommandHandler;

  @Override
  @Transactional
  public Optional<UpdatePersonResult> handle(UpdatePersonCommand command) {
    return personRepository
        .findById(command.personId())
        .map(
            person -> {
              command.info().applyTo(person);
              syncPersonRolesCommandHandler.handle(
                  new SyncPersonRolesCommand(
                      person.getId(), command.organizationId(), command.roles()));
              return new UpdatePersonResult(personRepository.save(person).getId());
            });
  }
}
