package org.ouracademy.ovejafiel.iam.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.command.add_owner.AddOwnerCommand;
import org.ouracademy.ovejafiel.iam.command.add_owner.AddOwnerCommandHandler;
import org.ouracademy.ovejafiel.iam.command.add_owner.AddOwnerResult;
import org.ouracademy.ovejafiel.iam.command.create_role.CreateRoleCommand;
import org.ouracademy.ovejafiel.iam.command.create_role.CreateRoleCommandHandler;
import org.ouracademy.ovejafiel.iam.command.create_role.CreateRoleResult;
import org.ouracademy.ovejafiel.iam.command.delete_role.DeleteRoleCommand;
import org.ouracademy.ovejafiel.iam.command.delete_role.DeleteRoleCommandHandler;
import org.ouracademy.ovejafiel.iam.command.remove_owner.RemoveOwnerCommand;
import org.ouracademy.ovejafiel.iam.command.remove_owner.RemoveOwnerCommandHandler;
import org.ouracademy.ovejafiel.iam.command.update_role.UpdateRoleCommandHandler;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.iam.domain.RoleBindingId;
import org.ouracademy.ovejafiel.iam.query.RoleResult;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.iam.query.find_role_by_id.FindRoleByIdQueryHandler;
import org.ouracademy.ovejafiel.iam.query.get_permission_catalog.GetPermissionCatalogQueryHandler;
import org.ouracademy.ovejafiel.iam.query.get_permission_catalog.PermissionCatalogResult;
import org.ouracademy.ovejafiel.iam.query.list_access.AccessEntryResult;
import org.ouracademy.ovejafiel.iam.query.list_access.ListAccessQuery;
import org.ouracademy.ovejafiel.iam.query.list_access.ListAccessQueryHandler;
import org.ouracademy.ovejafiel.iam.query.list_predefined_roles.ListPredefinedRolesQueryHandler;
import org.ouracademy.ovejafiel.iam.query.list_roles.ListRolesQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(AccessController.class)
@ContextConfiguration(classes = OvejafielApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class AccessControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean ListAccessQueryHandler listAccessQueryHandler;
  @MockitoBean ListRolesQueryHandler listRolesQueryHandler;
  @MockitoBean FindRoleByIdQueryHandler findRoleByIdQueryHandler;
  @MockitoBean GetPermissionCatalogQueryHandler getPermissionCatalogQueryHandler;
  @MockitoBean ListPredefinedRolesQueryHandler listPredefinedRolesQueryHandler;
  @MockitoBean AddOwnerCommandHandler addOwnerCommandHandler;
  @MockitoBean RemoveOwnerCommandHandler removeOwnerCommandHandler;
  @MockitoBean CreateRoleCommandHandler createRoleCommandHandler;
  @MockitoBean UpdateRoleCommandHandler updateRoleCommandHandler;
  @MockitoBean DeleteRoleCommandHandler deleteRoleCommandHandler;
  @MockitoBean FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  @MockitoBean AuthorizeQueryHandler authorizeQueryHandler;

  PartyId node = PartyId.generate();
  PartyId church = PartyId.generate();

  String bearer() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  void denyOn(PartyId organization, Permission permission) {
    given(authorizeQueryHandler.handle(new AuthorizeQuery("test-user", organization, permission)))
        .willThrow(new AccessDeniedException("no"));
  }

  void churchIs(PartyId church) {
    given(findChurchOfOrganizationQueryHandler.handle(any()))
        .willReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
  }

  @Test
  void listsTheAccessOfANodeWithIamRead() {
    given(listAccessQueryHandler.handle(new ListAccessQuery(node)))
        .willReturn(
            List.of(
                new AccessEntryResult(
                    RoleBindingId.generate(),
                    UserId.generate(),
                    "a@x.org",
                    PredefinedRoles.OWNER,
                    "Propietario",
                    church,
                    "Iglesia",
                    true,
                    AccessEntryResult.Source.DIRECT,
                    null,
                    12)));

    assertThat(
            mvc.get()
                .uri("/v1/organizations/" + node.value() + "/access")
                .header("Authorization", bearer()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].inherited")
        .isEqualTo(true);
    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", node, Permission.IAM_READ));
  }

  @Test
  void withoutIamReadTheAccessListIsForbidden() {
    denyOn(node, Permission.IAM_READ);

    assertThat(
            mvc.get()
                .uri("/v1/organizations/" + node.value() + "/access")
                .header("Authorization", bearer()))
        .hasStatus(403);
    verifyNoInteractions(listAccessQueryHandler);
  }

  @Test
  void addingAnOwnerNeedsIamManageAndPassesTheCallerSubject() {
    given(addOwnerCommandHandler.handle(any())).willReturn(new AddOwnerResult(true));
    var uri = "/v1/organizations/" + node.value() + "/owners";

    assertThat(
            mvc.post()
                .uri(uri)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@x.org\"}"))
        .hasStatus(201);
    verify(addOwnerCommandHandler).handle(new AddOwnerCommand("test-user", "a@x.org", node));

    denyOn(node, Permission.IAM_MANAGE);
    assertThat(
            mvc.post()
                .uri(uri)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@x.org\"}"))
        .hasStatus(403);
  }

  @Test
  void anOwnerNeedsAValidEmail() {
    assertThat(
            mvc.post()
                .uri("/v1/organizations/" + node.value() + "/owners")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\"}"))
        .hasStatus(400);
  }

  @Test
  void removingAnOwnerNeedsIamManage() {
    var binding = RoleBindingId.generate();
    var uri = "/v1/organizations/" + node.value() + "/owners/" + binding.value();

    assertThat(mvc.delete().uri(uri).header("Authorization", bearer())).hasStatus(204);
    verify(removeOwnerCommandHandler).handle(new RemoveOwnerCommand("test-user", binding, node));

    denyOn(node, Permission.IAM_MANAGE);
    assertThat(mvc.delete().uri(uri).header("Authorization", bearer())).hasStatus(403);
  }

  @Test
  void thePredefinedLevelsExcludeOwner() {
    given(listPredefinedRolesQueryHandler.handle(any()))
        .willReturn(
            List.of(
                new RoleResult(
                    PredefinedRoles.OWNER,
                    "Propietario",
                    null,
                    List.of("org.read"),
                    null,
                    true,
                    false),
                new RoleResult(
                    PredefinedRoles.LEADER, "Líder", null, List.of("org.read"), null, true, true)));

    assertThat(mvc.get().uri("/v1/iam/levels").header("Authorization", bearer()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[*].name")
        .isEqualTo(List.of("Líder"));
  }

  @Test
  void rolesAreCreatedOnTheChurchOfTheNodeWithIamManage() {
    churchIs(church);
    var id = IamRoleId.generate();
    given(createRoleCommandHandler.handle(any()))
        .willReturn(
            new CreateRoleResult(
                new RoleResult(id, "Tesorero", null, List.of("org.read"), church, false, true)));

    assertThat(
            mvc.post()
                .uri("/v1/organizations/" + node.value() + "/roles")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Tesorero\",\"permissions\":[\"org.read\"]}"))
        .hasStatus(201)
        .bodyJson()
        .extractingPath("$.id")
        .isEqualTo(id.value().toString());
    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", church, Permission.IAM_MANAGE));
    verify(createRoleCommandHandler)
        .handle(new CreateRoleCommand("test-user", church, "Tesorero", null, List.of("org.read")));

    denyOn(church, Permission.IAM_MANAGE);
    assertThat(
            mvc.post()
                .uri("/v1/organizations/" + node.value() + "/roles")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Otro\",\"permissions\":[\"org.read\"]}"))
        .hasStatus(403);
  }

  @Test
  void aRoleWithoutPermissionsFieldIsABadRequest() {
    churchIs(church);

    assertThat(
            mvc.post()
                .uri("/v1/organizations/" + node.value() + "/roles")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Otro\"}"))
        .hasStatus(400);
  }

  @Test
  void editingAndDeletingNeedIamManageOnTheRolesChurch() {
    var id = IamRoleId.generate();
    given(findRoleByIdQueryHandler.handle(any()))
        .willReturn(
            Optional.of(new RoleResult(id, "T", null, List.of("org.read"), church, false, true)));
    denyOn(church, Permission.IAM_MANAGE);

    assertThat(
            mvc.put()
                .uri("/v1/roles/" + id.value())
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"T\",\"permissions\":[\"org.read\"]}"))
        .hasStatus(403);
    assertThat(mvc.delete().uri("/v1/roles/" + id.value()).header("Authorization", bearer()))
        .hasStatus(403);
    verifyNoInteractions(updateRoleCommandHandler, deleteRoleCommandHandler);
  }

  @Test
  void deletingARoleCallsTheHandler() {
    var id = IamRoleId.generate();
    given(findRoleByIdQueryHandler.handle(any()))
        .willReturn(
            Optional.of(new RoleResult(id, "T", null, List.of("org.read"), church, false, true)));

    assertThat(mvc.delete().uri("/v1/roles/" + id.value()).header("Authorization", bearer()))
        .hasStatus(204);
    verify(deleteRoleCommandHandler).handle(new DeleteRoleCommand(id));
  }

  @Test
  void anUnknownRoleIsNotFound() {
    given(findRoleByIdQueryHandler.handle(any())).willReturn(Optional.empty());

    assertThat(
            mvc.delete()
                .uri("/v1/roles/" + IamRoleId.generate().value())
                .header("Authorization", bearer()))
        .hasStatus(404);
  }

  @Test
  void exposesThePermissionCatalog() {
    given(getPermissionCatalogQueryHandler.handle(any()))
        .willReturn(
            new PermissionCatalogResult(
                List.of(new PermissionCatalogResult.Group("org", List.of("org.read")))));

    assertThat(mvc.get().uri("/v1/iam/permissions").header("Authorization", bearer()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].resource")
        .isEqualTo("org");
  }
}
