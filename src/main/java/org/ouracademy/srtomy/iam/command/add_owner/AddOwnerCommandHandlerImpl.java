package org.ouracademy.srtomy.iam.command.add_owner;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.iam.command.GrantRules;
import org.ouracademy.srtomy.iam.command.grant_role.GrantRoleCommand;
import org.ouracademy.srtomy.iam.command.grant_role.GrantRoleCommandHandler;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
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
class AddOwnerCommandHandlerImpl implements AddOwnerCommandHandler {

  private final IamRoleRepository iamRoleRepository;
  private final GrantRules grantRules;
  private final GrantRoleCommandHandler grantRoleCommandHandler;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  private final FindUserByEmailQueryHandler findUserByEmailQueryHandler;
  private final FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;

  @Override
  @Transactional
  public AddOwnerResult handle(AddOwnerCommand command) {
    var churchId =
        findChurchOfOrganizationQueryHandler
            .handle(new FindChurchOfOrganizationQuery(command.organizationId()))
            .orElseThrow(() -> new NoSuchElementException("Organization not found"))
            .churchId();
    var owner =
        iamRoleRepository
            .findById(PredefinedRoles.OWNER)
            .orElseThrow(() -> new NoSuchElementException("Role not found"));
    grantRules.requireCanGrant(command.subject(), owner.getId(), owner.getPermissions(), churchId);

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
            new GrantRoleCommand(target.id(), owner.getId(), churchId, caller.id()));
    return new AddOwnerResult(granted.created());
  }
}
