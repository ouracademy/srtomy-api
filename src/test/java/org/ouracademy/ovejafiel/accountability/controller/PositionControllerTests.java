package org.ouracademy.ovejafiel.accountability.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.accountability.command.create_position.CreatePositionCommand;
import org.ouracademy.ovejafiel.accountability.command.create_position.CreatePositionCommandHandler;
import org.ouracademy.ovejafiel.accountability.command.create_position.CreatePositionResult;
import org.ouracademy.ovejafiel.accountability.command.delete_position.DeletePositionCommand;
import org.ouracademy.ovejafiel.accountability.command.delete_position.DeletePositionCommandHandler;
import org.ouracademy.ovejafiel.accountability.command.rename_position.RenamePositionCommandHandler;
import org.ouracademy.ovejafiel.accountability.command.set_position_access.SetPositionAccessCommand;
import org.ouracademy.ovejafiel.accountability.command.set_position_access.SetPositionAccessCommandHandler;
import org.ouracademy.ovejafiel.accountability.command.set_position_access.SetPositionAccessResult;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.query.PositionHolderResult;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.accountability.query.PositionSummaryResult;
import org.ouracademy.ovejafiel.accountability.query.find_position_by_id.FindPositionByIdQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.get_position_suggestions.GetPositionSuggestionsQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.get_position_suggestions.PositionSuggestionsResult;
import org.ouracademy.ovejafiel.accountability.query.get_positions_of_church.GetPositionsOfChurchQuery;
import org.ouracademy.ovejafiel.accountability.query.get_positions_of_church.GetPositionsOfChurchQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.get_positions_of_person.GetPositionsOfPersonQueryHandler;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(PositionController.class)
@ContextConfiguration(classes = OvejafielApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class PositionControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean GetPositionsOfChurchQueryHandler getPositionsOfChurchQueryHandler;
  @MockitoBean GetPositionsOfPersonQueryHandler getPositionsOfPersonQueryHandler;
  @MockitoBean GetPositionSuggestionsQueryHandler getPositionSuggestionsQueryHandler;
  @MockitoBean FindPositionByIdQueryHandler findPositionByIdQueryHandler;
  @MockitoBean CreatePositionCommandHandler createPositionCommandHandler;
  @MockitoBean RenamePositionCommandHandler renamePositionCommandHandler;
  @MockitoBean DeletePositionCommandHandler deletePositionCommandHandler;
  @MockitoBean SetPositionAccessCommandHandler setPositionAccessCommandHandler;
  @MockitoBean FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  @MockitoBean AuthorizeQueryHandler authorizeQueryHandler;

  String bearer() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  @Test
  void anyNodeListsThePositionsOfItsChurch() {
    var location = PartyId.generate();
    var church = PartyId.generate();
    given(findChurchOfOrganizationQueryHandler.handle(any()))
        .willReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    given(getPositionsOfChurchQueryHandler.handle(new GetPositionsOfChurchQuery(church)))
        .willReturn(
            List.of(
                new PositionSummaryResult(
                    new PositionResult(
                        AccountabilityTypeId.generate(), "Pastor", church, Set.of(), Set.of()),
                    List.of("org.read", "members.read"),
                    List.of(
                        new PositionHolderResult(
                            PartyId.generate(), "Ana Díaz", "Sede Central", "ana@x.org"),
                        new PositionHolderResult(
                            PartyId.generate(), "Luis Rojas", "Sede Norte", null)))));

    assertThat(
            mvc.get()
                .uri("/v1/organizations/" + location.value() + "/positions")
                .header("Authorization", bearer()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].holders.length()")
        .isEqualTo(2);
    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", location, Permission.ORG_READ));
  }

  @Test
  void creatingNeedsPositionsManageOnTheChurch() {
    var church = PartyId.generate();
    given(findChurchOfOrganizationQueryHandler.handle(any()))
        .willReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    given(
            authorizeQueryHandler.handle(
                new AuthorizeQuery("test-user", church, Permission.POSITIONS_MANAGE)))
        .willThrow(new AccessDeniedException("no"));

    assertThat(
            mvc.post()
                .uri("/v1/organizations/" + church.value() + "/positions")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Diácono\"}"))
        .hasStatus(403);
    verifyNoInteractions(createPositionCommandHandler);
  }

  @Test
  void createReturns201WithThePosition() {
    var church = PartyId.generate();
    var id = AccountabilityTypeId.generate();
    given(findChurchOfOrganizationQueryHandler.handle(any()))
        .willReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    given(
            createPositionCommandHandler.handle(
                new CreatePositionCommand("test-user", church, "Diácono", Set.of(), Set.of())))
        .willReturn(
            new CreatePositionResult(
                new PositionResult(id, "Diácono", church, Set.of(), Set.of())));

    assertThat(
            mvc.post()
                .uri("/v1/organizations/" + church.value() + "/positions")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Diácono\"}"))
        .hasStatus(201)
        .bodyJson()
        .extractingPath("$.id")
        .isEqualTo(id.value().toString());
  }

  @Test
  void deletingAnUnknownPositionIsNotFound() {
    given(findPositionByIdQueryHandler.handle(any())).willReturn(Optional.empty());

    assertThat(
            mvc.delete()
                .uri("/v1/positions/" + AccountabilityTypeId.generate().value())
                .header("Authorization", bearer()))
        .hasStatus(404);
    verifyNoInteractions(deletePositionCommandHandler);
  }

  @Test
  void deletingAKnownPositionChecksItsChurch() {
    var church = PartyId.generate();
    var id = AccountabilityTypeId.generate();
    given(findPositionByIdQueryHandler.handle(any()))
        .willReturn(Optional.of(new PositionResult(id, "Ujier", church, Set.of(), Set.of())));

    assertThat(mvc.delete().uri("/v1/positions/" + id.value()).header("Authorization", bearer()))
        .hasStatus(204);
    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", church, Permission.POSITIONS_MANAGE));
    verify(deletePositionCommandHandler).handle(new DeletePositionCommand("test-user", id));
  }

  @Test
  void suggestionsNeedNoNode() {
    given(getPositionSuggestionsQueryHandler.handle(any()))
        .willReturn(
            new PositionSuggestionsResult(
                List.of(
                    new PositionSuggestionsResult.Suggestion("Pastor", List.of("org.read")),
                    new PositionSuggestionsResult.Suggestion("Intercesor", List.of()))));

    assertThat(mvc.get().uri("/v1/positions/suggestions").header("Authorization", bearer()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].permissions[0]")
        .isEqualTo("org.read");
  }

  @Test
  void creatingWithRolesAndPermissionsPassesThemToTheHandler() {
    var church = PartyId.generate();
    var id = AccountabilityTypeId.generate();
    given(findChurchOfOrganizationQueryHandler.handle(any()))
        .willReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    given(
            createPositionCommandHandler.handle(
                new CreatePositionCommand(
                    "test-user",
                    church,
                    "Pastor",
                    Set.of(PredefinedRoles.ADMIN),
                    Set.of("events.write"))))
        .willReturn(
            new CreatePositionResult(
                new PositionResult(
                    id, "Pastor", church, Set.of(PredefinedRoles.ADMIN), Set.of("events.write"))));

    assertThat(
            mvc.post()
                .uri("/v1/organizations/" + church.value() + "/positions")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Pastor\",\"roleIds\":[\""
                        + PredefinedRoles.ADMIN.value()
                        + "\"],\"permissions\":[\"events.write\"]}"))
        .hasStatus(201)
        .bodyJson()
        .extractingPath("$.permissions[0]")
        .isEqualTo("events.write");
  }

  @Test
  void anEscalationAttemptOnCreateIsForbidden() {
    var church = PartyId.generate();
    given(findChurchOfOrganizationQueryHandler.handle(any()))
        .willReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    given(createPositionCommandHandler.handle(any())).willThrow(new AccessDeniedException("no"));

    assertThat(
            mvc.post()
                .uri("/v1/organizations/" + church.value() + "/positions")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Pastor\"}"))
        .hasStatus(403);
  }

  @Test
  void settingTheAccessOfAPositionPassesTheSubjectAndWhatItGives() {
    var church = PartyId.generate();
    var id = AccountabilityTypeId.generate();
    given(findPositionByIdQueryHandler.handle(any()))
        .willReturn(Optional.of(new PositionResult(id, "Ujier", church, Set.of(), Set.of())));
    given(
            setPositionAccessCommandHandler.handle(
                new SetPositionAccessCommand(
                    "test-user", id, Set.of(), Set.of("attendance.write"))))
        .willReturn(
            Optional.of(
                new SetPositionAccessResult(
                    new PositionResult(
                        id, "Ujier", church, Set.of(), Set.of("attendance.write")))));

    assertThat(
            mvc.put()
                .uri("/v1/positions/" + id.value() + "/access")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"permissions\":[\"attendance.write\"]}"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.permissions[0]")
        .isEqualTo("attendance.write");
    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", church, Permission.POSITIONS_MANAGE));
  }

  @Test
  void removingTheAccessOfAPositionSendsANullLevel() {
    var church = PartyId.generate();
    var id = AccountabilityTypeId.generate();
    given(findPositionByIdQueryHandler.handle(any()))
        .willReturn(Optional.of(new PositionResult(id, "Ujier", church, Set.of(), Set.of())));
    given(
            setPositionAccessCommandHandler.handle(
                new SetPositionAccessCommand("test-user", id, Set.of(), Set.of())))
        .willReturn(
            Optional.of(
                new SetPositionAccessResult(
                    new PositionResult(id, "Ujier", church, Set.of(), Set.of()))));

    assertThat(
            mvc.put()
                .uri("/v1/positions/" + id.value() + "/access")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .hasStatusOk();
  }

  @Test
  void settingTheAccessWithoutPositionsManageIsForbidden() {
    var church = PartyId.generate();
    var id = AccountabilityTypeId.generate();
    given(findPositionByIdQueryHandler.handle(any()))
        .willReturn(Optional.of(new PositionResult(id, "Ujier", church, Set.of(), Set.of())));
    given(
            authorizeQueryHandler.handle(
                new AuthorizeQuery("test-user", church, Permission.POSITIONS_MANAGE)))
        .willThrow(new AccessDeniedException("no"));

    assertThat(
            mvc.put()
                .uri("/v1/positions/" + id.value() + "/access")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .hasStatus(403);
    verifyNoInteractions(setPositionAccessCommandHandler);
  }
}
