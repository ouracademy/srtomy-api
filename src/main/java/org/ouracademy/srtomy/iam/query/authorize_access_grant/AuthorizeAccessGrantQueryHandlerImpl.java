package org.ouracademy.srtomy.iam.query.authorize_access_grant;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.iam.command.GrantRules;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.srtomy.iam.repository.IamRoleRepository;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class AuthorizeAccessGrantQueryHandlerImpl implements AuthorizeAccessGrantQueryHandler {

  private final AuthorizeQueryHandler authorizeQueryHandler;
  private final IamRoleRepository iamRoleRepository;
  private final GrantRules grantRules;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;

  @Override
  @Transactional(readOnly = true)
  public AuthorizeAccessGrantResult handle(AuthorizeAccessGrantQuery query) {
    authorizeQueryHandler.handle(
        new AuthorizeQuery(query.subject(), query.organizationId(), Permission.IAM_MANAGE));
    var role =
        iamRoleRepository
            .findById(query.roleId())
            .orElseThrow(() -> new NoSuchElementException("Role not found"));
    if (role.getId().equals(PredefinedRoles.OWNER)) {
      throw new DomainValidationException(
          "El nivel Propietario no se asigna a un cargo; se agrega desde Propietarios");
    }
    var churchId =
        findChurchOfOrganizationQueryHandler
            .handle(new FindChurchOfOrganizationQuery(query.organizationId()))
            .orElseThrow(() -> new NoSuchElementException("Organization not found"))
            .churchId();
    if (!role.isPredefined() && !role.getChurch().getId().equals(churchId)) {
      throw new DomainValidationException("Ese nivel de acceso no pertenece a esta iglesia");
    }
    grantRules.requireCanGrant(
        query.subject(), role.getId(), role.getPermissions(), query.organizationId());
    return new AuthorizeAccessGrantResult(role.getId());
  }
}
