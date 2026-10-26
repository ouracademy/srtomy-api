package org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetPersonAccountabilitiesQueryHandlerImpl implements GetPersonAccountabilitiesQueryHandler {

  private final AccountabilityRepository accountabilityRepository;

  @Override
  @Transactional(readOnly = true)
  public List<PersonAccountabilityResult> handle(GetPersonAccountabilitiesQuery query) {
    return accountabilityRepository.findByChildId(query.personId()).stream()
        .map(
            accountability ->
                new PersonAccountabilityResult(
                    accountability.getParent().getId(),
                    accountability.getType().getId(),
                    accountability.getType().isPosition()
                        ? PositionResult.from(accountability.getType())
                        : null))
        .toList();
  }
}
