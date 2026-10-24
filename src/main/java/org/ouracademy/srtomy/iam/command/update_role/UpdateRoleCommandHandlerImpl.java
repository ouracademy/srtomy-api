package org.ouracademy.srtomy.iam.command.update_role;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.iam.command.RoleRules;
import org.ouracademy.srtomy.iam.query.RoleResult;
import org.ouracademy.srtomy.iam.repository.IamRoleRepository;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UpdateRoleCommandHandlerImpl implements UpdateRoleCommandHandler {

  private final IamRoleRepository iamRoleRepository;
  private final RoleRules roleRules;

  @Override
  @Transactional
  public UpdateRoleResult handle(UpdateRoleCommand command) {
    var role =
        iamRoleRepository
            .findById(command.roleId())
            .orElseThrow(() -> new NoSuchElementException("Role not found"));
    if (role.isPredefined()) {
      throw new DomainValidationException("Los roles predefinidos no se pueden editar");
    }
    var churchId = role.getChurch().getId();
    var name = roleRules.requireAvailableName(churchId, command.name(), role.getId());
    var permissions =
        roleRules.requirePermissions(command.subject(), churchId, command.permissions());
    role.update(name, command.description(), permissions);
    return new UpdateRoleResult(RoleResult.from(role));
  }
}
