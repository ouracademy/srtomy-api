package org.ouracademy.ovejafiel.iam.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.iam.command.add_owner.AddOwnerCommand;
import org.ouracademy.ovejafiel.iam.command.add_owner.AddOwnerCommandHandler;
import org.ouracademy.ovejafiel.iam.command.create_role.CreateRoleCommand;
import org.ouracademy.ovejafiel.iam.command.create_role.CreateRoleCommandHandler;
import org.ouracademy.ovejafiel.iam.command.delete_role.DeleteRoleCommand;
import org.ouracademy.ovejafiel.iam.command.delete_role.DeleteRoleCommandHandler;
import org.ouracademy.ovejafiel.iam.command.remove_owner.RemoveOwnerCommand;
import org.ouracademy.ovejafiel.iam.command.remove_owner.RemoveOwnerCommandHandler;
import org.ouracademy.ovejafiel.iam.command.update_role.UpdateRoleCommand;
import org.ouracademy.ovejafiel.iam.command.update_role.UpdateRoleCommandHandler;
import org.ouracademy.ovejafiel.iam.controller.dto.AccessEntryResponse;
import org.ouracademy.ovejafiel.iam.controller.dto.OwnerRequest;
import org.ouracademy.ovejafiel.iam.controller.dto.PermissionGroupResponse;
import org.ouracademy.ovejafiel.iam.controller.dto.RoleRequest;
import org.ouracademy.ovejafiel.iam.controller.dto.RoleResponse;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.domain.RoleBindingId;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.iam.query.find_role_by_id.FindRoleByIdQuery;
import org.ouracademy.ovejafiel.iam.query.find_role_by_id.FindRoleByIdQueryHandler;
import org.ouracademy.ovejafiel.iam.query.get_permission_catalog.GetPermissionCatalogQuery;
import org.ouracademy.ovejafiel.iam.query.get_permission_catalog.GetPermissionCatalogQueryHandler;
import org.ouracademy.ovejafiel.iam.query.list_access.ListAccessQuery;
import org.ouracademy.ovejafiel.iam.query.list_access.ListAccessQueryHandler;
import org.ouracademy.ovejafiel.iam.query.list_predefined_roles.ListPredefinedRolesQuery;
import org.ouracademy.ovejafiel.iam.query.list_predefined_roles.ListPredefinedRolesQueryHandler;
import org.ouracademy.ovejafiel.iam.query.list_roles.ListRolesQuery;
import org.ouracademy.ovejafiel.iam.query.list_roles.ListRolesQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
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
 * The "Accesos" tab: who has access to a node and where it comes from (a position, group leadership
 * or being an Owner), the Owners of a church, and the access levels each church can give to its
 * positions. Reading needs {@code iam.read}, changing needs {@code iam.manage}; the escalation
 * rules live in the handlers.
 *
 * <p>Access is not handed out one by one: it comes from the access level of a position (see the
 * positions endpoints). The only direct access left is Owner.
 */
@RestController
@AllArgsConstructor
@Tag(name = "iam")
public class AccessController {

  ListAccessQueryHandler listAccessQueryHandler;
  ListRolesQueryHandler listRolesQueryHandler;
  FindRoleByIdQueryHandler findRoleByIdQueryHandler;
  GetPermissionCatalogQueryHandler getPermissionCatalogQueryHandler;
  ListPredefinedRolesQueryHandler listPredefinedRolesQueryHandler;
  AddOwnerCommandHandler addOwnerCommandHandler;
  RemoveOwnerCommandHandler removeOwnerCommandHandler;
  CreateRoleCommandHandler createRoleCommandHandler;
  UpdateRoleCommandHandler updateRoleCommandHandler;
  DeleteRoleCommandHandler deleteRoleCommandHandler;
  FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  AuthorizeQueryHandler authorizeQueryHandler;

  /** The accesses on the node plus the ones inherited from its ancestors. */
  @GetMapping("organizations/{id}/access")
  public List<AccessEntryResponse> access(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    var organizationId = new PartyId(id);
    authorize(jwt, organizationId, Permission.IAM_READ);
    return listAccessQueryHandler.handle(new ListAccessQuery(organizationId)).stream()
        .map(AccessEntryResponse::from)
        .toList();
  }

  /** Makes someone an Owner of the church of {@code id}. Only an Owner can. */
  @PostMapping("organizations/{id}/owners")
  @ResponseStatus(HttpStatus.CREATED)
  public void addOwner(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid OwnerRequest request) {
    var organizationId = new PartyId(id);
    authorize(jwt, organizationId, Permission.IAM_MANAGE);
    addOwnerCommandHandler.handle(
        new AddOwnerCommand(jwt.getSubject(), request.email(), organizationId));
  }

  @DeleteMapping("organizations/{id}/owners/{bindingId}")
  public ResponseEntity<Void> removeOwner(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID bindingId) {
    var organizationId = new PartyId(id);
    authorize(jwt, organizationId, Permission.IAM_MANAGE);
    removeOwnerCommandHandler.handle(
        new RemoveOwnerCommand(jwt.getSubject(), new RoleBindingId(bindingId), organizationId));
    return ResponseEntity.noContent().build();
  }

  /**
   * The predefined access levels, for forms that run before the church exists (the sign-up). Any
   * signed-in user can read them.
   */
  @GetMapping("iam/levels")
  public List<RoleResponse> levels() {
    return listPredefinedRolesQueryHandler.handle(new ListPredefinedRolesQuery()).stream()
        .filter(role -> role.assignable())
        .map(RoleResponse::from)
        .toList();
  }

  /** The access levels of the church of {@code id}: predefined plus its own. */
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
