package org.ouracademy.ovejafiel.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQuery;
import org.ouracademy.ovejafiel.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQueryHandler;
import org.ouracademy.ovejafiel.iam.query.get_effective_permissions.GetEffectivePermissionsQuery;
import org.ouracademy.ovejafiel.iam.query.get_effective_permissions.GetEffectivePermissionsQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.TokenClaims;
import org.ouracademy.ovejafiel.user.command.sync_current_user.SyncCurrentUserCommand;
import org.ouracademy.ovejafiel.user.command.sync_current_user.SyncCurrentUserCommandHandler;
import org.ouracademy.ovejafiel.user.controller.dto.MeResponse;
import org.ouracademy.ovejafiel.user.controller.dto.MyOrganizationResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user: who they are, which churches they can reach and what they may do. */
@RestController
@RequestMapping("me")
@AllArgsConstructor
@Tag(name = "me")
public class MeController {

  SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  FindAccessibleOrganizationsQueryHandler findAccessibleOrganizationsQueryHandler;
  GetEffectivePermissionsQueryHandler getEffectivePermissionsQueryHandler;

  /**
   * Gets the signed-in user. Registers them on their first sign-in and refreshes their email
   * afterwards.
   */
  @GetMapping
  public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
    var result =
        syncCurrentUserCommandHandler.handle(
            new SyncCurrentUserCommand(
                jwt.getSubject(), TokenClaims.email(jwt), TokenClaims.emailVerified(jwt)));
    return MeResponse.from(result.user());
  }

  /**
   * Lists the churches, locations, groups and ministries the signed-in user can reach, with the
   * parent of each one and the roles the user has there. Start here to find the ids of the nodes.
   */
  @GetMapping("organizations")
  public List<MyOrganizationResponse> organizations(@AuthenticationPrincipal Jwt jwt) {
    return findAccessibleOrganizationsQueryHandler
        .handle(new FindAccessibleOrganizationsQuery(jwt.getSubject()))
        .stream()
        .map(MyOrganizationResponse::from)
        .toList();
  }

  /**
   * Lists the permissions the signed-in user holds on a node, inherited ones included. They are
   * values such as {@code members.read} or {@code events.write}.
   *
   * @param organizationId Id of the node: a church, location, group or ministry.
   */
  @GetMapping("permissions")
  public List<String> permissions(
      @AuthenticationPrincipal Jwt jwt, @RequestParam UUID organizationId) {
    return getEffectivePermissionsQueryHandler
        .handle(new GetEffectivePermissionsQuery(jwt.getSubject(), new PartyId(organizationId)))
        .permissions()
        .stream()
        .map(Permission::value)
        .toList();
  }
}
