package org.ouracademy.ovejafiel.iam.command.grant_role;

import jakarta.persistence.EntityManager;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.iam.domain.RoleBinding;
import org.ouracademy.ovejafiel.iam.repository.IamRoleRepository;
import org.ouracademy.ovejafiel.iam.repository.RoleBindingRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GrantRoleCommandHandlerImpl implements GrantRoleCommandHandler {

  private final RoleBindingRepository roleBindingRepository;
  private final IamRoleRepository iamRoleRepository;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public GrantRoleResult handle(GrantRoleCommand command) {
    if (roleBindingRepository.existsByUserIdAndRoleIdAndOrganizationId(
        command.userId(), command.roleId(), command.organizationId())) {
      return new GrantRoleResult(false);
    }
    var role =
        iamRoleRepository
            .findById(command.roleId())
            .orElseThrow(
                () -> new NoSuchElementException("Role not found: " + command.roleId().value()));
    var grantedBy =
        command.grantedBy() == null
            ? null
            : entityManager.getReference(AppUser.class, command.grantedBy());
    roleBindingRepository.save(
        new RoleBinding(
            entityManager.getReference(AppUser.class, command.userId()),
            role,
            entityManager.getReference(Organization.class, command.organizationId()),
            grantedBy));
    return new GrantRoleResult(true);
  }
}
