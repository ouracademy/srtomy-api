package org.ouracademy.srtomy.organization.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.accountability.domain.AccountabilityId;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.AccountabilityTypeResult;
import org.ouracademy.srtomy.accountability.query.get_predefined_roles.GetPredefinedRolesQuery;
import org.ouracademy.srtomy.accountability.query.get_predefined_roles.GetPredefinedRolesQueryHandler;
import org.ouracademy.srtomy.accountability.query.get_roles_of_location.GetRolesOfLocationQueryHandler;
import org.ouracademy.srtomy.accountability.query.get_roles_of_person.GetRolesOfPersonQueryHandler;
import org.ouracademy.srtomy.organization.command.OrganizationInfo;
import org.ouracademy.srtomy.organization.command.create_church.CreateChurchCommand;
import org.ouracademy.srtomy.organization.command.create_church.CreateChurchCommandHandler;
import org.ouracademy.srtomy.organization.command.create_church.CreateChurchResult;
import org.ouracademy.srtomy.organization.command.create_location.CreateLocationCommandHandler;
import org.ouracademy.srtomy.organization.command.create_location_child.CreateLocationChildCommandHandler;
import org.ouracademy.srtomy.organization.command.delete_organization.DeleteOrganizationCommand;
import org.ouracademy.srtomy.organization.command.delete_organization.DeleteOrganizationCommandHandler;
import org.ouracademy.srtomy.organization.command.delete_organization.DeleteOrganizationResult;
import org.ouracademy.srtomy.organization.command.delete_organization.DeleteOrganizationResult.Outcome;
import org.ouracademy.srtomy.organization.command.edit_organization.EditOrganizationCommandHandler;
import org.ouracademy.srtomy.organization.command.update_location.UpdateLocationCommandHandler;
import org.ouracademy.srtomy.organization.command.update_location_child.UpdateLocationChildCommandHandler;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.ouracademy.srtomy.organization.query.PersonDetails;
import org.ouracademy.srtomy.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.srtomy.organization.query.find_organization_children.FindOrganizationChildrenQueryHandler;
import org.ouracademy.srtomy.organization.query.get_location_children.GetLocationChildrenQuery;
import org.ouracademy.srtomy.organization.query.get_location_children.GetLocationChildrenQueryHandler;
import org.ouracademy.srtomy.organization.query.get_location_children.GetLocationChildrenResult;
import org.ouracademy.srtomy.organization.query.get_member_state.GetMemberStateQuery;
import org.ouracademy.srtomy.organization.query.get_member_state.GetMemberStateQueryHandler;
import org.ouracademy.srtomy.organization.query.get_member_state.MemberStateResult;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.shared.SecurityConfig;
import org.ouracademy.srtomy.shared.SrtomyApplication;
import org.ouracademy.srtomy.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(OrganizationController.class)
@ContextConfiguration(classes = SrtomyApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class OrganizationControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean CreateChurchCommandHandler createChurchCommandHandler;
  @MockitoBean CreateLocationCommandHandler createLocationCommandHandler;
  @MockitoBean UpdateLocationCommandHandler updateLocationCommandHandler;
  @MockitoBean CreateLocationChildCommandHandler createLocationChildCommandHandler;
  @MockitoBean UpdateLocationChildCommandHandler updateLocationChildCommandHandler;
  @MockitoBean EditOrganizationCommandHandler editOrganizationCommandHandler;
  @MockitoBean DeleteOrganizationCommandHandler deleteOrganizationCommandHandler;
  @MockitoBean FindOrganizationByIdQueryHandler findOrganizationByIdQueryHandler;
  @MockitoBean FindOrganizationChildrenQueryHandler findOrganizationChildrenQueryHandler;
  @MockitoBean GetLocationChildrenQueryHandler getLocationChildrenQueryHandler;
  @MockitoBean GetMemberStateQueryHandler getMemberStateQueryHandler;
  @MockitoBean GetPredefinedRolesQueryHandler getPredefinedRolesQueryHandler;
  @MockitoBean GetRolesOfLocationQueryHandler getRolesOfLocationQueryHandler;
  @MockitoBean GetRolesOfPersonQueryHandler getRolesOfPersonQueryHandler;

  static final PersonDetails ANA_DETAILS =
      new PersonDetails("Ana", "Diaz", "999", 30, LocalDate.of(1995, 5, 17), true);

  String bearer() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  static FindOrganizationResult church(PartyId id) {
    return new FindOrganizationResult(
        id, "Church", "desc", "addr", "123", Organization.Type.CHURCH, null, null);
  }

  @Test
  void predefinedRolesReturnsTheSeededRoles() {
    given(getPredefinedRolesQueryHandler.handle(new GetPredefinedRolesQuery()))
        .willReturn(
            List.of(
                new AccountabilityTypeResult(AccountabilityTypeId.generate(), "PASTOR", true),
                new AccountabilityTypeResult(
                    AccountabilityTypeId.generate(), "WORSHIP_LEADER", true),
                new AccountabilityTypeResult(AccountabilityTypeId.generate(), "USHER", true)));

    assertThat(
            mvc.get()
                .uri("/v1/organizations/locations/predefined-roles")
                .header("Authorization", bearer()))
        .hasStatusOk()
        .bodyText()
        .contains("PASTOR", "WORSHIP_LEADER", "USHER");
  }

  @Test
  void createMapsTheRequestToACreateChurchCommandAndReturnsTheChurch() {
    var id = PartyId.generate();
    given(
            createChurchCommandHandler.handle(
                new CreateChurchCommand(
                    new OrganizationInfo("Church", "desc", "addr", "123"), null, "auth0|123")))
        .willReturn(new CreateChurchResult(church(id)));

    assertThat(
            mvc.post()
                .uri("/v1/organizations")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Church","description":"desc","address":"addr",
                     "phoneNumber":"123","userId":"auth0|123"}
                    """))
        .hasStatus(201)
        .bodyJson()
        .extractingPath("$.id")
        .isEqualTo(id.value().toString());
  }

  @Test
  void findByIdOfAnUnknownOrganizationIsNotFound() {
    given(findOrganizationByIdQueryHandler.handle(any())).willReturn(Optional.empty());

    assertThat(
            mvc.get()
                .uri("/v1/organizations/" + PartyId.generate().value())
                .header("Authorization", bearer()))
        .hasStatus(404);
  }

  @Test
  void editOfAnUnknownOrganizationIsNotFound() {
    given(editOrganizationCommandHandler.handle(any())).willReturn(Optional.empty());

    assertThat(
            mvc.put()
                .uri("/v1/organizations/" + PartyId.generate().value())
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Church\"}"))
        .hasStatus(404);
  }

  @Test
  void createLocationWithoutRolesIsBadRequest() {
    assertThat(
            mvc.post()
                .uri("/v1/organizations/locations")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Sede\",\"parentId\":\""
                        + PartyId.generate().value()
                        + "\",\"roles\":[]}"))
        .hasStatus(400);
  }

  @Test
  void updateLocationWithoutRolesReachesTheHandler() {
    given(updateLocationCommandHandler.handle(any())).willReturn(Optional.empty());

    assertThat(
            mvc.put()
                .uri("/v1/organizations/locations/" + PartyId.generate().value())
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sede\",\"roles\":[]}"))
        .hasStatus(404);
  }

  @Test
  void updateOfAnUnknownGroupIsNotFound() {
    given(updateLocationChildCommandHandler.handle(any())).willReturn(Optional.empty());

    assertThat(
            mvc.put()
                .uri("/v1/organizations/locations/childs/" + PartyId.generate().value())
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Youth\",\"type\":\"MINISTRY\","
                        + "\"leaderIds\":[\""
                        + PartyId.generate().value()
                        + "\"],"
                        + "\"memberIds\":[\""
                        + PartyId.generate().value()
                        + "\"]}"))
        .hasStatus(404);
  }

  @Test
  void updateRejectsAChurchOrLocationType() {
    assertThat(
            mvc.put()
                .uri("/v1/organizations/locations/childs/" + PartyId.generate().value())
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Church\",\"type\":\"CHURCH\","
                        + "\"leaderIds\":[\""
                        + PartyId.generate().value()
                        + "\"],"
                        + "\"memberIds\":[\""
                        + PartyId.generate().value()
                        + "\"]}"))
        .hasStatus(400);
  }

  @Test
  void deleteMapsEachOutcomeToItsStatus() {
    var id = PartyId.generate();
    var uri = "/v1/organizations/" + id.value();
    var command = new DeleteOrganizationCommand(id);

    given(deleteOrganizationCommandHandler.handle(command))
        .willReturn(new DeleteOrganizationResult(Outcome.DELETED));
    assertThat(mvc.delete().uri(uri).header("Authorization", bearer())).hasStatus(204);

    given(deleteOrganizationCommandHandler.handle(command))
        .willReturn(new DeleteOrganizationResult(Outcome.NOT_DELETABLE));
    assertThat(mvc.delete().uri(uri).header("Authorization", bearer())).hasStatus(400);

    given(deleteOrganizationCommandHandler.handle(command))
        .willReturn(new DeleteOrganizationResult(Outcome.NOT_FOUND));
    assertThat(mvc.delete().uri(uri).header("Authorization", bearer())).hasStatus(404);
  }

  @Test
  void locationChildMembersAreGroupedByTypeName() {
    var groupId = PartyId.generate();
    var memberId = PartyId.generate();
    given(getLocationChildrenQueryHandler.handle(new GetLocationChildrenQuery(groupId)))
        .willReturn(
            new GetLocationChildrenResult(
                Map.of(
                    "IS_PART_OF",
                    List.of(
                        new GetLocationChildrenResult.Child(
                            memberId,
                            "Ana Diaz",
                            GetLocationChildrenResult.Kind.PERSON,
                            ANA_DETAILS),
                        new GetLocationChildrenResult.Child(
                            PartyId.generate(),
                            "Kids",
                            GetLocationChildrenResult.Kind.ORGANIZATION,
                            null)))));

    var response =
        assertThat(
                mvc.get()
                    .uri("/v1/organizations/locations/childs/" + groupId.value() + "/members")
                    .header("Authorization", bearer()))
            .hasStatusOk()
            .bodyJson();
    response.extractingPath("$.IS_PART_OF[0].name").isEqualTo("Ana Diaz");
    response.extractingPath("$.IS_PART_OF[0].kind").isEqualTo("PERSON");
    response.extractingPath("$.IS_PART_OF[0].firstName").isEqualTo("Ana");
    response.extractingPath("$.IS_PART_OF[0].lastName").isEqualTo("Diaz");
    response.extractingPath("$.IS_PART_OF[0].age").isEqualTo(30);
    response.extractingPath("$.IS_PART_OF[0].birthdate").isEqualTo("1995-05-17");
    response.extractingPath("$.IS_PART_OF[1].kind").isEqualTo("ORGANIZATION");
    response.extractingPath("$.IS_PART_OF[1].firstName").isNull();
  }

  @Test
  void memberStateKeepsTheNestedChildAndTypeNames() {
    var orgId = PartyId.generate();
    given(getMemberStateQueryHandler.handle(new GetMemberStateQuery(orgId)))
        .willReturn(
            List.of(
                new MemberStateResult(
                    AccountabilityId.generate(),
                    PartyId.generate(),
                    "Ana",
                    orgId,
                    AccountabilityTypeId.generate(),
                    "VISITOR",
                    ANA_DETAILS)));

    var response =
        assertThat(
                mvc.get()
                    .uri("/v1/organizations/" + orgId.value() + "/member-state")
                    .header("Authorization", bearer()))
            .hasStatusOk()
            .bodyJson();
    response.extractingPath("$[0].child.name").isEqualTo("Ana");
    response.extractingPath("$[0].type.name").isEqualTo("VISITOR");
    response.extractingPath("$[0].child.firstName").isEqualTo("Ana");
    response.extractingPath("$[0].child.lastName").isEqualTo("Diaz");
    response.extractingPath("$[0].child.age").isEqualTo(30);
    response.extractingPath("$[0].child.birthdate").isEqualTo("1995-05-17");
  }
}
