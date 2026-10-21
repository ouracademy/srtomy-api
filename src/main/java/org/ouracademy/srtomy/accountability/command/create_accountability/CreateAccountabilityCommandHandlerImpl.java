package org.ouracademy.srtomy.accountability.command.create_accountability;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.domain.Accountability;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.party.domain.Party;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreateAccountabilityCommandHandlerImpl implements CreateAccountabilityCommandHandler {

  private final AccountabilityRepository accountabilityRepository;
  private final AccountabilityTypeRepository accountabilityTypeRepository;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public CreateAccountabilityResult handle(CreateAccountabilityCommand command) {
    var accountability =
        accountabilityRepository.save(
            new Accountability(
                entityManager.getReference(Party.class, command.childId()),
                entityManager.getReference(Party.class, command.parentId()),
                accountabilityTypeRepository.getReferenceById(command.typeId())));
    return new CreateAccountabilityResult(accountability.getId());
  }
}
