package org.ouracademy.ovejafiel.person.query.find_person_by_id;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.person.query.FindPersonResult;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindPersonByIdQueryHandlerImpl implements FindPersonByIdQueryHandler {

  private final PersonRepository personRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<FindPersonResult> handle(FindPersonByIdQuery query) {
    return personRepository.findById(query.id()).map(FindPersonResult::from);
  }
}
