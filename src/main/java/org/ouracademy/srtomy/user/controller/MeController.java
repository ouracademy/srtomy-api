package org.ouracademy.srtomy.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQuery;
import org.ouracademy.srtomy.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQueryHandler;
import org.ouracademy.srtomy.iam.query.get_effective_permissions.GetEffectivePermissionsQuery;
import org.ouracademy.srtomy.iam.query.get_effective_permissions.GetEffectivePermissionsQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.shared.TokenClaims;
import org.ouracademy.srtomy.user.command.sync_current_user.SyncCurrentUserCommand;
import org.ouracademy.srtomy.user.command.sync_current_user.SyncCurrentUserCommandHandler;
import org.ouracademy.srtomy.user.controller.dto.MeResponse;
import org.ouracademy.srtomy.user.controller.dto.MyOrganizationResponse;
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

  /** Registers the user on first sign-in and refreshes their email afterwards. */
  @GetMapping
  public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
    var result =
        syncCurrentUserCommandHandler.handle(
            new SyncCurrentUserCommand(
                jwt.getSubject(), TokenClaims.email(jwt), TokenClaims.emailVerified(jwt)));
    return MeResponse.from(result.user());
  }

  @GetMapping("organizations")
  public List<MyOrganizationResponse> organizations(@AuthenticationPrincipal Jwt jwt) {
    return findAccessibleOrganizationsQueryHandler
        .handle(new FindAccessibleOrganizationsQuery(jwt.getSubject()))
        .stream()
        .map(MyOrganizationResponse::from)
        .toList();
  }

  /** Permission values (e.g. {@code members.read}) the user holds on the node, inherited too. */
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
