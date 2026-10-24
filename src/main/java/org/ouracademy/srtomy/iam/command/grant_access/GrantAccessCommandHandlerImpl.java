package org.ouracademy.srtomy.iam.command.grant_access;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.iam.command.GrantRules;
import org.ouracademy.srtomy.iam.command.grant_role.GrantRoleCommand;
import org.ouracademy.srtomy.iam.command.grant_role.GrantRoleCommandHandler;
import org.ouracademy.srtomy.iam.repository.IamRoleRepository;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.ouracademy.srtomy.user.query.find_user_by_email.FindUserByEmailQuery;
import org.ouracademy.srtomy.user.query.find_user_by_email.FindUserByEmailQueryHandler;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GrantAccessCommandHandlerImpl implements GrantAccessCommandHandler {

  private final IamRoleRepository iamRoleRepository;
  private final GrantRules grantRules;
  private final GrantRoleCommandHandler grantRoleCommandHandler;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  private final FindUserByEmailQueryHandler findUserByEmailQueryHandler;
  private final FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;

  @Override
  @Transactional
  public GrantAccessResult handle(GrantAccessCommand command) {
    var role =
        iamRoleRepository
            .findById(command.roleId())
            .orElseThrow(() -> new NoSuchElementException("Role not found"));
    var churchId =
        findChurchOfOrganizationQueryHandler
            .handle(new FindChurchOfOrganizationQuery(command.organizationId()))
            .orElseThrow(() -> new NoSuchElementException("Organization not found"))
            .churchId();
    if (!role.isPredefined() && !role.getChurch().getId().equals(churchId)) {
      throw new DomainValidationException("Ese rol no pertenece a esta iglesia");
    }
    grantRules.requireCanGrant(
        command.subject(), role.getId(), role.getPermissions(), command.organizationId());

    var caller =
        findUserBySubjectQueryHandler
            .handle(new FindUserBySubjectQuery(command.subject()))
            .orElseThrow(() -> new AccessDeniedException("Usuario no registrado"));
    var target =
        findUserByEmailQueryHandler
            .handle(new FindUserByEmailQuery(command.email()))
            .orElseThrow(
                () ->
                    new DomainValidationException(
                        "No hay un usuario con ese correo verificado. Debe iniciar sesión primero"));
    var granted =
        grantRoleCommandHandler.handle(
            new GrantRoleCommand(target.id(), role.getId(), command.organizationId(), caller.id()));
    return new GrantAccessResult(granted.created());
  }
}
