package org.ouracademy.srtomy.iam.command.create_role;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.iam.command.RoleRules;
import org.ouracademy.srtomy.iam.domain.IamRole;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.iam.query.RoleResult;
import org.ouracademy.srtomy.iam.repository.IamRoleRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreateRoleCommandHandlerImpl implements CreateRoleCommandHandler {

  private final IamRoleRepository iamRoleRepository;
  private final RoleRules roleRules;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public CreateRoleResult handle(CreateRoleCommand command) {
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
