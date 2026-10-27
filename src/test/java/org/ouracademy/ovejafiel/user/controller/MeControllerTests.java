package org.ouracademy.ovejafiel.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.find_accessible_organizations.AccessibleOrganizationResult;
import org.ouracademy.ovejafiel.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQuery;
import org.ouracademy.ovejafiel.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQueryHandler;
import org.ouracademy.ovejafiel.iam.query.get_effective_permissions.EffectivePermissionsResult;
import org.ouracademy.ovejafiel.iam.query.get_effective_permissions.GetEffectivePermissionsQuery;
import org.ouracademy.ovejafiel.iam.query.get_effective_permissions.GetEffectivePermissionsQueryHandler;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.ouracademy.ovejafiel.user.command.sync_current_user.SyncCurrentUserCommand;
import org.ouracademy.ovejafiel.user.command.sync_current_user.SyncCurrentUserCommandHandler;
import org.ouracademy.ovejafiel.user.command.sync_current_user.SyncCurrentUserResult;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(MeController.class)
@ContextConfiguration(classes = OvejafielApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class MeControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  @MockitoBean FindAccessibleOrganizationsQueryHandler findAccessibleOrganizationsQueryHandler;
  @MockitoBean GetEffectivePermissionsQueryHandler getEffectivePermissionsQueryHandler;

  String bearer() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  @Test
  void meRegistersTheUserFromTheToken() {
    var id = UserId.generate();
    given(
            syncCurrentUserCommandHandler.handle(
                new SyncCurrentUserCommand("test-user", null, false)))
        .willReturn(new SyncCurrentUserResult(new UserResult(id, "test-user", null, false), true));

    assertThat(mvc.get().uri("/v1/me").header("Authorization", bearer()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.id")
        .isEqualTo(id.value().toString());
  }

  @Test
  void organizationsListsTheNodesWithTheirParentAndRoles() {
    var church = PartyId.generate();
    var location = PartyId.generate();
    given(
            findAccessibleOrganizationsQueryHandler.handle(
                new FindAccessibleOrganizationsQuery("test-user")))
        .willReturn(
            List.of(
                new AccessibleOrganizationResult(
                    church,
                    "Iglesia",
                    Organization.Type.CHURCH,
                    null,
                    true,
                    List.of("Propietario")),
                new AccessibleOrganizationResult(
                    location, "Sede", Organization.Type.LOCATION, church, true, List.of())));

    var body =
        assertThat(mvc.get().uri("/v1/me/organizations").header("Authorization", bearer()))
            .hasStatusOk()
            .bodyJson();
    body.extractingPath("$[0].roles[0]").isEqualTo("Propietario");
    body.extractingPath("$[1].parentId").isEqualTo(church.value().toString());
  }

  @Test
  void permissionsAreReturnedAsTheirWireValues() {
    var node = PartyId.generate();
    given(
            getEffectivePermissionsQueryHandler.handle(
                new GetEffectivePermissionsQuery("test-user", node)))
        .willReturn(
            new EffectivePermissionsResult(
                EnumSet.of(Permission.ORG_READ, Permission.MEMBERS_WRITE)));

    assertThat(
            mvc.get()
                .uri("/v1/me/permissions?organizationId=" + node.value())
                .header("Authorization", bearer()))
        .hasStatusOk()
        .bodyText()
        .contains("org.read", "members.write");
  }
}
