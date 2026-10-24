package org.ouracademy.srtomy.accountability.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.accountability.command.create_position.CreatePositionCommand;
import org.ouracademy.srtomy.accountability.command.create_position.CreatePositionCommandHandler;
import org.ouracademy.srtomy.accountability.command.create_position.CreatePositionResult;
import org.ouracademy.srtomy.accountability.command.delete_position.DeletePositionCommand;
import org.ouracademy.srtomy.accountability.command.delete_position.DeletePositionCommandHandler;
import org.ouracademy.srtomy.accountability.command.rename_position.RenamePositionCommandHandler;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.PositionResult;
import org.ouracademy.srtomy.accountability.query.find_position_by_id.FindPositionByIdQueryHandler;
import org.ouracademy.srtomy.accountability.query.get_position_suggestions.GetPositionSuggestionsQueryHandler;
import org.ouracademy.srtomy.accountability.query.get_position_suggestions.PositionSuggestionsResult;
import org.ouracademy.srtomy.accountability.query.get_positions_of_church.GetPositionsOfChurchQuery;
import org.ouracademy.srtomy.accountability.query.get_positions_of_church.GetPositionsOfChurchQueryHandler;
import org.ouracademy.srtomy.accountability.query.get_positions_of_person.GetPositionsOfPersonQueryHandler;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.shared.SecurityConfig;
import org.ouracademy.srtomy.shared.SrtomyApplication;
import org.ouracademy.srtomy.shared.TestSecurityConfig;
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
@ContextConfiguration(classes = SrtomyApplication.class)
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
        .willReturn(List.of(new PositionResult(AccountabilityTypeId.generate(), "Pastor", church)));

    assertThat(
            mvc.get()
                .uri("/v1/organizations/" + location.value() + "/positions")
                .header("Authorization", bearer()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].name")
        .isEqualTo("Pastor");
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
    given(createPositionCommandHandler.handle(new CreatePositionCommand(church, "Diácono")))
        .willReturn(new CreatePositionResult(new PositionResult(id, "Diácono", church)));

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
        .willReturn(Optional.of(new PositionResult(id, "Ujier", church)));

    assertThat(mvc.delete().uri("/v1/positions/" + id.value()).header("Authorization", bearer()))
        .hasStatus(204);
    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", church, Permission.POSITIONS_MANAGE));
    verify(deletePositionCommandHandler).handle(new DeletePositionCommand(id));
  }

  @Test
  void suggestionsNeedNoNode() {
    given(getPositionSuggestionsQueryHandler.handle(any()))
        .willReturn(new PositionSuggestionsResult(List.of("Pastor", "Catequista")));

    assertThat(mvc.get().uri("/v1/positions/suggestions").header("Authorization", bearer()))
        .hasStatusOk()
        .bodyText()
        .contains("Catequista");
  }
}
