package org.ouracademy.ovejafiel.accountability.command.set_node_membership;

import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.LEADER;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER_PROGRESS_STATES;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.command.MemberStateEvents;
import org.ouracademy.ovejafiel.accountability.command.set_member_state.SetMemberStateCommand;
import org.ouracademy.ovejafiel.accountability.command.set_member_state.SetMemberStateCommandHandler;
import org.ouracademy.ovejafiel.accountability.domain.Accountability;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.ovejafiel.party.domain.Party;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SetNodeMembershipCommandHandlerImpl implements SetNodeMembershipCommandHandler {

  private final AccountabilityRepository accountabilityRepository;
  private final AccountabilityTypeRepository accountabilityTypeRepository;
  private final SetMemberStateCommandHandler setMemberStateCommandHandler;
  private final MemberStateEvents memberStateEvents;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public SetNodeMembershipResult handle(SetNodeMembershipCommand command) {
    if (!List.of(MEMBER_PROGRESS_STATES).contains(command.state())) {
      throw new DomainValidationException("El estado de la persona no es válido");
    }
    var held =
        accountabilityRepository.findByParentIdAndChildId(
            command.organizationId(), command.personId());
    if (!command.state().equals(MEMBER)
        && held.stream().anyMatch(found -> found.getType().isPosition())) {
      throw new DomainValidationException(
          "Quien tiene un cargo es miembro: quita el cargo para cambiar su estado");
    }

    var joined = !isPartOf(command.organizationId(), command.personId());
    if (joined) {
      join(command.personId(), command.organizationId(), command.state());
    } else {
      setMemberStateCommandHandler.handle(
          new SetMemberStateCommand(command.personId(), command.organizationId(), command.state()));
    }
    setLeader(command, held);
    return new SetNodeMembershipResult(joined);
  }

  private boolean isPartOf(PartyId organizationId, PartyId personId) {
    return accountabilityRepository.findByParentIdAndChildId(organizationId, personId).stream()
        .anyMatch(found -> found.getType().getId().equals(IS_PART_OF));
  }

  private void join(PartyId personId, PartyId organizationId, AccountabilityTypeId state) {
    var person = entityManager.getReference(Party.class, personId);
    var organization = entityManager.getReference(Party.class, organizationId);
    accountabilityRepository.saveAll(
        List.of(
            new Accountability(
                person, organization, accountabilityTypeRepository.getReferenceById(IS_PART_OF)),
            new Accountability(
                person, organization, accountabilityTypeRepository.getReferenceById(state))));
    memberStateEvents.joined(
        personId, organizationId, SystemAccountabilityTypes.progressStateName(state).orElseThrow());
  }

  private void setLeader(SetNodeMembershipCommand command, List<Accountability> held) {
    var current = held.stream().filter(found -> found.getType().getId().equals(LEADER)).toList();
    if (command.leader() && current.isEmpty()) {
      accountabilityRepository.save(
          new Accountability(
              entityManager.getReference(Party.class, command.personId()),
              entityManager.getReference(Party.class, command.organizationId()),
              accountabilityTypeRepository.getReferenceById(LEADER)));
    } else if (!command.leader() && !current.isEmpty()) {
      accountabilityRepository.deleteAll(current);
    }
  }
}
