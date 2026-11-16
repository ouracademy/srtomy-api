package org.ouracademy.ovejafiel.person.query.count_people;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CountPeopleQueryHandlerImpl implements CountPeopleQueryHandler {

  private final PersonRepository personRepository;

  @Override
  @Transactional(readOnly = true)
  public CountPeopleResult handle(CountPeopleQuery query) {
    return new CountPeopleResult(
        personRepository.countActiveOfChurch(
            query.churchId(), java.util.List.of(SystemAccountabilityTypes.MEMBER_PROGRESS_STATES)));
  }
}
