package org.ouracademy.srtomy.iam.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.srtomy.iam.command.create_role.CreateRoleCommand;
import org.ouracademy.srtomy.iam.command.create_role.CreateRoleCommandHandler;
import org.ouracademy.srtomy.iam.command.delete_role.DeleteRoleCommand;
import org.ouracademy.srtomy.iam.command.delete_role.DeleteRoleCommandHandler;
import org.ouracademy.srtomy.iam.command.grant_access.GrantAccessCommand;
import org.ouracademy.srtomy.iam.command.grant_access.GrantAccessCommandHandler;
import org.ouracademy.srtomy.iam.command.revoke_access.RevokeAccessCommand;
import org.ouracademy.srtomy.iam.command.revoke_access.RevokeAccessCommandHandler;
import org.ouracademy.srtomy.iam.command.update_role.UpdateRoleCommand;
import org.ouracademy.srtomy.iam.command.update_role.UpdateRoleCommandHandler;
import org.ouracademy.srtomy.iam.controller.dto.AccessEntryResponse;
import org.ouracademy.srtomy.iam.controller.dto.GrantAccessRequest;
import org.ouracademy.srtomy.iam.controller.dto.PermissionGroupResponse;
import org.ouracademy.srtomy.iam.controller.dto.RoleRequest;
import org.ouracademy.srtomy.iam.controller.dto.RoleResponse;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.domain.RoleBindingId;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.srtomy.iam.query.find_role_by_id.FindRoleByIdQuery;
import org.ouracademy.srtomy.iam.query.find_role_by_id.FindRoleByIdQueryHandler;
import org.ouracademy.srtomy.iam.query.get_permission_catalog.GetPermissionCatalogQuery;
import org.ouracademy.srtomy.iam.query.get_permission_catalog.GetPermissionCatalogQueryHandler;
import org.ouracademy.srtomy.iam.query.list_access.ListAccessQuery;
import org.ouracademy.srtomy.iam.query.list_access.ListAccessQueryHandler;
import org.ouracademy.srtomy.iam.query.list_roles.ListRolesQuery;
import org.ouracademy.srtomy.iam.query.list_roles.ListRolesQueryHandler;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The "Accesos" tab: who holds which role on a node (directly or inherited), granting and revoking
 * roles, and each church's custom roles. Reading needs {@code iam.read}, changing needs {@code
 * iam.manage}; the escalation rules live in the command handlers.
 */
@RestController
@AllArgsConstructor
@Tag(name = "iam")
public class AccessController {

  ListAccessQueryHandler listAccessQueryHandler;
  ListRolesQueryHandler listRolesQueryHandler;
  FindRoleByIdQueryHandler findRoleByIdQueryHandler;
  GetPermissionCatalogQueryHandler getPermissionCatalogQueryHandler;
  GrantAccessCommandHandler grantAccessCommandHandler;
  RevokeAccessCommandHandler revokeAccessCommandHandler;
  CreateRoleCommandHandler createRoleCommandHandler;
  UpdateRoleCommandHandler updateRoleCommandHandler;
  DeleteRoleCommandHandler deleteRoleCommandHandler;
  FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  AuthorizeQueryHandler authorizeQueryHandler;

  /** Bindings made on the node plus the ones inherited from its ancestors. */
  @GetMapping("organizations/{id}/access")
  public List<AccessEntryResponse> access(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    var organizationId = new PartyId(id);
    authorize(jwt, organizationId, Permission.IAM_READ);
    return listAccessQueryHandler.handle(new ListAccessQuery(organizationId)).stream()
        .map(AccessEntryResponse::from)
        .toList();
  }

  @PostMapping("organizations/{id}/access")
  @ResponseStatus(HttpStatus.CREATED)
  public void grant(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid GrantAccessRequest request) {
    var organizationId = new PartyId(id);
    authorize(jwt, organizationId, Permission.IAM_MANAGE);
    grantAccessCommandHandler.handle(
        new GrantAccessCommand(
            jwt.getSubject(), request.email(), new IamRoleId(request.roleId()), organizationId));
  }

  @DeleteMapping("organizations/{id}/access/{bindingId}")
  public ResponseEntity<Void> revoke(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID bindingId) {
    var organizationId = new PartyId(id);
    authorize(jwt, organizationId, Permission.IAM_MANAGE);
    revokeAccessCommandHandler.handle(
        new RevokeAccessCommand(jwt.getSubject(), new RoleBindingId(bindingId), organizationId));
    return ResponseEntity.noContent().build();
  }

  /** The roles that can be granted in the church of {@code id}: predefined plus custom. */
  @GetMapping("organizations/{id}/roles")
  public List<RoleResponse> roles(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    var organizationId = new PartyId(id);
    authorize(jwt, organizationId, Permission.IAM_READ);
    return listRolesQueryHandler.handle(new ListRolesQuery(churchOf(organizationId))).stream()
        .map(RoleResponse::from)
        .toList();
  }

  @PostMapping("organizations/{id}/roles")
  @ResponseStatus(HttpStatus.CREATED)
  public RoleResponse createRole(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid RoleRequest request) {
    var churchId = churchOf(new PartyId(id));
    authorize(jwt, churchId, Permission.IAM_MANAGE);
    return RoleResponse.from(
        createRoleCommandHandler
            .handle(
                new CreateRoleCommand(
                    jwt.getSubject(),
                    churchId,
                    request.name(),
                    request.description(),
                    request.permissions()))
            .role());
  }

  @PutMapping("roles/{roleId}")
  public RoleResponse updateRole(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID roleId,
      @RequestBody @Valid RoleRequest request) {
    var id = new IamRoleId(roleId);
    authorizeOnRole(jwt, id);
    return RoleResponse.from(
        updateRoleCommandHandler
            .handle(
                new UpdateRoleCommand(
                    jwt.getSubject(),
                    id,
                    request.name(),
                    request.description(),
                    request.permissions()))
            .role());
  }

  @DeleteMapping("roles/{roleId}")
  public ResponseEntity<Void> deleteRole(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID roleId) {
    var id = new IamRoleId(roleId);
    authorizeOnRole(jwt, id);
    deleteRoleCommandHandler.handle(new DeleteRoleCommand(id));
    return ResponseEntity.noContent().build();
  }

  /** Every permission a role may include, grouped by resource, to build the selection list. */
  @GetMapping("iam/permissions")
  public List<PermissionGroupResponse> permissions() {
    return getPermissionCatalogQueryHandler
        .handle(new GetPermissionCatalogQuery())
        .groups()
        .stream()
        .map(group -> new PermissionGroupResponse(group.resource(), group.permissions()))
        .toList();
  }

  private void authorize(Jwt jwt, PartyId organizationId, Permission permission) {
    authorizeQueryHandler.handle(new AuthorizeQuery(jwt.getSubject(), organizationId, permission));
  }

  /** 404 when the role does not exist, 403 without {@code iam.manage} on its church. */
  private void authorizeOnRole(Jwt jwt, IamRoleId roleId) {
    var role =
        findRoleByIdQueryHandler
            .handle(new FindRoleByIdQuery(roleId))
            .orElseThrow(() -> new NoSuchElementException("Role not found"));
    if (role.predefined()) {
      // Predefined roles belong to no church, so nobody manages them (the handler says why).
      return;
    }
    authorize(jwt, role.churchId(), Permission.IAM_MANAGE);
  }

  private PartyId churchOf(PartyId organizationId) {
    return findChurchOfOrganizationQueryHandler
        .handle(new FindChurchOfOrganizationQuery(organizationId))
        .orElseThrow(() -> new NoSuchElementException("Organization not found"))
        .churchId();
  }
}
