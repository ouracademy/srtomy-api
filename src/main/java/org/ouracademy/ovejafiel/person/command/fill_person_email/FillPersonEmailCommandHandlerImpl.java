package org.ouracademy.ovejafiel.person.command.fill_person_email;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FillPersonEmailCommandHandlerImpl implements FillPersonEmailCommandHandler {

  private final PersonRepository personRepository;

  @Override
  @Transactional
  public FillPersonEmailResult handle(FillPersonEmailCommand command) {
    var person =
        personRepository
            .findById(command.personId())
            .orElseThrow(() -> new NoSuchElementException("Person not found"));
    if (person.getEmail() != null && !person.getEmail().isBlank()) {
      return new FillPersonEmailResult(false);
    }
    person.setEmail(command.email().trim().toLowerCase());
    return new FillPersonEmailResult(true);
  }
}
