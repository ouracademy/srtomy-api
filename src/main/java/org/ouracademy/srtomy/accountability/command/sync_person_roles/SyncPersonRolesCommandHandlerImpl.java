package org.ouracademy.srtomy.accountability.command.sync_person_roles;

import com.google.common.collect.Sets;
import jakarta.persistence.EntityManager;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.domain.Accountability;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType.Category;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.party.domain.Party;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SyncPersonRolesCommandHandlerImpl implements SyncPersonRolesCommandHandler {

  private final AccountabilityTypeRepository accountabilityTypeRepository;
  private final AccountabilityRepository accountabilityRepository;
  private final EntityManager entityManager;

  @Override
  @Transactional(propagation = Propagation.REQUIRED)
  public SyncPersonRolesResult handle(SyncPersonRolesCommand command) {
    var roles =
        accountabilityRepository
            .findByParentIdAndChildId(command.locationId(), command.personId())
            .stream()
            .filter(
                x ->
                    x.getType().getCategory() != null
                        && Category.ROLE.equals(x.getType().getCategory()))
            .toList();
    var oldRoleTypeIds = roles.stream().map(x -> x.getType().getId()).collect(Collectors.toSet());

    // location.roles.name not in roles => delete those not in // A - B
    var rolesToDelete = Sets.difference(oldRoleTypeIds, command.newRoleTypeIds());
    var accountabilitiesToDelete =
        roles.stream().filter(x -> rolesToDelete.contains(x.getType().getId())).toList();
    accountabilityRepository.deleteAll(accountabilitiesToDelete);

    // new roles not in location.roles => add // B - A
    var rolesToAdd = Sets.difference(command.newRoleTypeIds(), oldRoleTypeIds);
    var person = entityManager.getReference(Party.class, command.personId());
    var location = entityManager.getReference(Party.class, command.locationId());
    var accountabilitiesToAdd =
        rolesToAdd.stream()
            .map(
                typeId ->
                    new Accountability(
                        person, location, accountabilityTypeRepository.getReferenceById(typeId)))
            .toList();
    accountabilityRepository.saveAll(accountabilitiesToAdd);

    return new SyncPersonRolesResult(accountabilitiesToAdd.size(), accountabilitiesToDelete.size());
  }
}
