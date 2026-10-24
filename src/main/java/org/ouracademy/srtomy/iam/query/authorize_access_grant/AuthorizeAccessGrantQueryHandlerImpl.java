package org.ouracademy.srtomy.iam.query.authorize_access_grant;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.iam.command.GrantRules;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.srtomy.iam.query.expand_permissions.ExpandPermissionsQuery;
import org.ouracademy.srtomy.iam.query.expand_permissions.ExpandPermissionsQueryHandler;
import org.ouracademy.srtomy.iam.repository.IamRoleRepository;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class AuthorizeAccessGrantQueryHandlerImpl implements AuthorizeAccessGrantQueryHandler {

  private final AuthorizeQueryHandler authorizeQueryHandler;
  private final IamRoleRepository iamRoleRepository;
  private final GrantRules grantRules;
  private final ExpandPermissionsQueryHandler expandPermissionsQueryHandler;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;

  @Override
  @Transactional(readOnly = true)
  public AuthorizeAccessGrantResult handle(AuthorizeAccessGrantQuery query) {
    authorizeQueryHandler.handle(
        new AuthorizeQuery(query.subject(), query.organizationId(), Permission.IAM_MANAGE));
    for (var permission : query.permissions()) {
      if (Permission.fromValue(permission).isEmpty()) {
        throw new DomainValidationException("Permiso desconocido: " + permission);
      }
    }
    var churchId = churchOf(query.organizationId());
    for (var roleId : query.roleIds()) {
      var role =
          iamRoleRepository
              .findById(roleId)
              .orElseThrow(() -> new NoSuchElementException("Role not found"));
      if (role.getId().equals(PredefinedRoles.OWNER)) {
        throw new DomainValidationException(
            "El rol Propietario no se asigna a un cargo; se agrega desde Propietarios");
      }
      if (!role.isPredefined() && !role.getChurch().getId().equals(churchId)) {
        throw new DomainValidationException("Ese rol no pertenece a esta iglesia");
      }
    }
    var effective =
        expandPermissionsQueryHandler
            .handle(new ExpandPermissionsQuery(query.roleIds(), query.permissions()))
            .permissions();
    grantRules.requireHoldsAll(query.subject(), effective, query.organizationId());
    return new AuthorizeAccessGrantResult(effective);
  }

  private PartyId churchOf(PartyId organizationId) {
    return findChurchOfOrganizationQueryHandler
        .handle(new FindChurchOfOrganizationQuery(organizationId))
        .orElseThrow(() -> new NoSuchElementException("Organization not found"))
        .churchId();
  }
}
