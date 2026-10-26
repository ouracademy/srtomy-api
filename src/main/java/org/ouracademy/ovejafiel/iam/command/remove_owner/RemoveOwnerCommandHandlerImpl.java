package org.ouracademy.ovejafiel.iam.command.remove_owner;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.iam.command.GrantRules;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.iam.repository.RoleBindingRepository;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class RemoveOwnerCommandHandlerImpl implements RemoveOwnerCommandHandler {

  private final RoleBindingRepository roleBindingRepository;
  private final GrantRules grantRules;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;

  @Override
  @Transactional
  public RemoveOwnerResult handle(RemoveOwnerCommand command) {
    var churchId =
        findChurchOfOrganizationQueryHandler
            .handle(new FindChurchOfOrganizationQuery(command.organizationId()))
            .orElseThrow(() -> new NoSuchElementException("Organization not found"))
            .churchId();
    var binding =
        roleBindingRepository
            .findById(command.bindingId())
            .filter(b -> b.getRole().getId().equals(PredefinedRoles.OWNER))
            .filter(b -> b.getOrganization().getId().equals(churchId))
            .orElseThrow(() -> new NoSuchElementException("Owner not found"));
    grantRules.requireCanRevoke(command.subject(), PredefinedRoles.OWNER, churchId);

    if (roleBindingRepository.countByRoleInChurch(PredefinedRoles.OWNER.value(), churchId.value())
        <= 1) {
      throw new DomainValidationException("La iglesia debe conservar al menos un Propietario");
    }
    roleBindingRepository.delete(binding);
    return new RemoveOwnerResult();
  }
}
