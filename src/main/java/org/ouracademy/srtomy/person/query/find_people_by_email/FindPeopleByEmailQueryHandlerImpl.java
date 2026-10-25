package org.ouracademy.srtomy.person.query.find_people_by_email;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.person.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindPeopleByEmailQueryHandlerImpl implements FindPeopleByEmailQueryHandler {

  private final PersonRepository personRepository;

  @Override
  @Transactional(readOnly = true)
  public FindPeopleByEmailResult handle(FindPeopleByEmailQuery query) {
    if (query.email() == null || query.email().isBlank()) {
      return new FindPeopleByEmailResult(List.of());
    }
    return new FindPeopleByEmailResult(
        personRepository
            .findByEmailAndOrganization(query.email().trim(), query.organizationId())
            .stream()
            .map(person -> person.getId())
            .toList());
  }
}
