package org.ouracademy.ovejafiel.accountability.command.set_member_state;

import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER_PROGRESS_STATES;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.Accountability;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.ovejafiel.party.domain.Party;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SetMemberStateCommandHandlerImpl implements SetMemberStateCommandHandler {

  private final AccountabilityRepository accountabilityRepository;
  private final AccountabilityTypeRepository accountabilityTypeRepository;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public SetMemberStateResult handle(SetMemberStateCommand command) {
    var states = List.of(MEMBER_PROGRESS_STATES);
    if (!states.contains(command.state())) {
      throw new DomainValidationException("El estado de la persona no es válido");
    }
    var current =
        accountabilityRepository
            .findByParentIdAndChildId(command.organizationId(), command.personId())
            .stream()
            .filter(found -> states.contains(found.getType().getId()))
            .toList();
    var alreadyThere = current.stream().anyMatch(found -> isState(found, command.state()));
    accountabilityRepository.deleteAll(
        current.stream()
            .filter(found -> !(alreadyThere && isState(found, command.state())))
            .toList());
    if (!alreadyThere) {
      accountabilityRepository.save(
          new Accountability(
              entityManager.getReference(Party.class, command.personId()),
              entityManager.getReference(Party.class, command.organizationId()),
              accountabilityTypeRepository.getReferenceById(command.state())));
    }
    return new SetMemberStateResult();
  }

  private static boolean isState(Accountability accountability, AccountabilityTypeId state) {
    return accountability.getType().getId().equals(state);
  }
}
