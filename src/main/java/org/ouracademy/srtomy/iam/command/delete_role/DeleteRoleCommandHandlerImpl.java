package org.ouracademy.srtomy.iam.command.delete_role;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.iam.repository.IamRoleRepository;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class DeleteRoleCommandHandlerImpl implements DeleteRoleCommandHandler {

  private final IamRoleRepository iamRoleRepository;

  /** Soft delete: its bindings stay in the table but stop counting. */
  @Override
  @Transactional
  public DeleteRoleResult handle(DeleteRoleCommand command) {
    var role =
        iamRoleRepository
            .findById(command.roleId())
            .orElseThrow(() -> new NoSuchElementException("Role not found"));
    if (role.isPredefined()) {
      throw new DomainValidationException("Los roles predefinidos no se pueden borrar");
    }
    role.delete();
    return new DeleteRoleResult();
  }
}
