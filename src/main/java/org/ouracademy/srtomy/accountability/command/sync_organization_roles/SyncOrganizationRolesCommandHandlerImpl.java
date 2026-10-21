package org.ouracademy.srtomy.accountability.command.sync_organization_roles;

import com.google.common.collect.Sets;
import jakarta.persistence.EntityManager;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType.Category;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.party.domain.Party;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SyncOrganizationRolesCommandHandlerImpl implements SyncOrganizationRolesCommandHandler {

  private final AccountabilityTypeRepository accountabilityTypeRepository;
  private final AccountabilityRepository accountabilityRepository;
  private final EntityManager entityManager;

  @Override
  @Transactional(propagation = Propagation.REQUIRED)
  public SyncOrganizationRolesResult handle(SyncOrganizationRolesCommand command) {
    var location = entityManager.getReference(Party.class, command.locationId());
    var roles = command.roleNames();
    var locationRoles = accountabilityTypeRepository.findByParty(location);
    var locationRoleNames =
        locationRoles.stream().map(x -> x.getName()).collect(Collectors.toSet());

    // location.roles.name not in roles => delete those not in // A - B
    var rolesToDelete = Sets.difference(locationRoleNames, roles);
    for (String roleNameToDelete : rolesToDelete) {
      var roleToDelete =
          locationRoles.stream()
              .filter(x -> x.getName().equals(roleNameToDelete))
              .findFirst()
              .orElseThrow();
      accountabilityTypeRepository.delete(roleToDelete);
      accountabilityRepository.deleteAllByType(roleToDelete);
    }

    // new roles not in location.roles => add // B - A
    var rolesToAdd = Sets.difference(roles, locationRoleNames);
    accountabilityTypeRepository.saveAll(
        rolesToAdd.stream()
            .map(roleName -> new AccountabilityType(roleName, location, Category.ROLE))
            .toList());

    return new SyncOrganizationRolesResult(rolesToAdd.size(), rolesToDelete.size());
  }
}
