package org.ouracademy.srtomy.person.query.find_people_by_ids;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.person.query.FindPersonResult;
import org.ouracademy.srtomy.person.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindPeopleByIdsQueryHandlerImpl implements FindPeopleByIdsQueryHandler {

  private final PersonRepository personRepository;

  @Override
  @Transactional(readOnly = true)
  public List<FindPersonResult> handle(FindPeopleByIdsQuery query) {
    if (query.ids().isEmpty()) {
      return List.of();
    }
    return personRepository.findAllById(query.ids()).stream().map(FindPersonResult::from).toList();
  }
}
