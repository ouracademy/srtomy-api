package org.ouracademy.ovejafiel.iam.command.create_role;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.domain.Feature;
import org.ouracademy.ovejafiel.billing.query.check_feature.CheckFeatureQuery;
import org.ouracademy.ovejafiel.billing.query.check_feature.CheckFeatureQueryHandler;
import org.ouracademy.ovejafiel.iam.command.RoleRules;
import org.ouracademy.ovejafiel.iam.domain.IamRole;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.query.RoleResult;
import org.ouracademy.ovejafiel.iam.repository.IamRoleRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreateRoleCommandHandlerImpl implements CreateRoleCommandHandler {

  private final IamRoleRepository iamRoleRepository;
  private final RoleRules roleRules;
  private final EntityManager entityManager;
  private final CheckFeatureQueryHandler checkFeature;

  @Override
  @Transactional
  public CreateRoleResult handle(CreateRoleCommand command) {
    // Only the predefined roles are in the free plan; creating one's own is from BASIC (402).
    checkFeature.handle(new CheckFeatureQuery(command.churchId(), Feature.CUSTOM_ROLES));
    var name =
        roleRules.requireAvailableName(command.churchId(), command.name(), IamRoleId.generate());
    var permissions =
        roleRules.requirePermissions(command.subject(), command.churchId(), command.permissions());
    var role =
        IamRole.custom(
            entityManager.getReference(Organization.class, command.churchId()),
            name,
            command.description(),
            permissions);
    return new CreateRoleResult(RoleResult.from(iamRoleRepository.save(role)));
  }
}
