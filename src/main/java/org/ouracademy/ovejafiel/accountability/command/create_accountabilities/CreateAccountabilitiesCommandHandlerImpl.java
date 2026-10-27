package org.ouracademy.ovejafiel.accountability.command.create_accountabilities;

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
class CreateAccountabilitiesCommandHandlerImpl implements CreateAccountabilitiesCommandHandler {

  private final AccountabilityRepository accountabilityRepository;
  private final AccountabilityTypeRepository accountabilityTypeRepository;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public CreateAccountabilitiesResult handle(CreateAccountabilitiesCommand command) {
    var accountabilities =
        command.items().stream()
            .map(
                item ->
                    new Accountability(
                        entityManager.getReference(Party.class, item.childId()),
                        entityManager.getReference(Party.class, item.parentId()),
                        accountabilityTypeRepository.getReferenceById(item.typeId())))
            .toList();
    var saved = accountabilityRepository.saveAll(accountabilities);
    return new CreateAccountabilitiesResult(saved.stream().map(Accountability::getId).toList());
  }
}
