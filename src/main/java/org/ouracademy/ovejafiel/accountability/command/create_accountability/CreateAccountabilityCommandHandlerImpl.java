package org.ouracademy.ovejafiel.accountability.command.create_accountability;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.Accountability;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.ovejafiel.party.domain.Party;
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
