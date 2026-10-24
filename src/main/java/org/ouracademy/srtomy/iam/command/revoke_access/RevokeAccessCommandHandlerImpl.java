package org.ouracademy.srtomy.iam.command.revoke_access;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.iam.command.GrantRules;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.repository.RoleBindingRepository;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class RevokeAccessCommandHandlerImpl implements RevokeAccessCommandHandler {

  private final RoleBindingRepository roleBindingRepository;
  private final GrantRules grantRules;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;

  @Override
  @Transactional
  public RevokeAccessResult handle(RevokeAccessCommand command) {
    var binding =
        roleBindingRepository
            .findById(command.bindingId())
            .filter(b -> b.getOrganization().getId().equals(command.organizationId()))
            .orElseThrow(() -> new NoSuchElementException("Access not found"));
    var roleId = binding.getRole().getId();
    grantRules.requireCanRevoke(command.subject(), roleId, command.organizationId());

    if (roleId.equals(PredefinedRoles.OWNER)) {
      var churchId =
          findChurchOfOrganizationQueryHandler
              .handle(new FindChurchOfOrganizationQuery(command.organizationId()))
              .orElseThrow(() -> new NoSuchElementException("Organization not found"))
              .churchId();
      if (roleBindingRepository.countByRoleInChurch(roleId.value(), churchId.value()) <= 1) {
        throw new DomainValidationException("La iglesia debe conservar al menos un Propietario");
      }
    }
    roleBindingRepository.delete(binding);
    return new RevokeAccessResult();
  }
}
