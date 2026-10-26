package org.ouracademy.ovejafiel.iam.command.delete_role;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.iam.repository.IamRoleRepository;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class DeleteRoleCommandHandlerImpl implements DeleteRoleCommandHandler {

  private final IamRoleRepository iamRoleRepository;

  /**
   * Soft delete. A level that a position still uses cannot go: that would silently take the access
   * away from everyone who holds the position.
   */
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
    var positions = iamRoleRepository.countPositionsUsing(role.getId().value());
    if (positions > 0) {
      throw new DomainValidationException(
          "Este nivel lo usan "
              + positions
              + (positions == 1 ? " cargo" : " cargos")
              + ". Cámbiales el nivel antes de borrarlo");
    }
    role.delete();
    return new DeleteRoleResult();
  }
}
