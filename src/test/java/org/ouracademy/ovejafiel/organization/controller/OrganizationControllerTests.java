package org.ouracademy.ovejafiel.organization.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth.GetMemberGrowthQuery;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth.GetMemberGrowthQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.get_member_growth.MemberGrowthResult;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.organization.command.OrganizationInfo;
import org.ouracademy.ovejafiel.organization.command.create_church.CreateChurchCommand;
import org.ouracademy.ovejafiel.organization.command.create_church.CreateChurchCommand.Creator;
import org.ouracademy.ovejafiel.organization.command.create_church.CreateChurchCommand.PositionSeed;
import org.ouracademy.ovejafiel.organization.command.create_church.CreateChurchCommandHandler;
import org.ouracademy.ovejafiel.organization.command.create_church.CreateChurchResult;
import org.ouracademy.ovejafiel.organization.command.create_location.CreateLocationCommandHandler;
import org.ouracademy.ovejafiel.organization.command.create_location_child.CreateLocationChildCommandHandler;
import org.ouracademy.ovejafiel.organization.command.delete_organization.DeleteOrganizationCommand;
import org.ouracademy.ovejafiel.organization.command.delete_organization.DeleteOrganizationCommandHandler;
import org.ouracademy.ovejafiel.organization.command.delete_organization.DeleteOrganizationResult;
import org.ouracademy.ovejafiel.organization.command.delete_organization.DeleteOrganizationResult.Outcome;
import org.ouracademy.ovejafiel.organization.command.edit_organization.EditOrganizationCommandHandler;
import org.ouracademy.ovejafiel.organization.command.update_location.UpdateLocationCommandHandler;
import org.ouracademy.ovejafiel.organization.command.update_location_child.UpdateLocationChildCommandHandler;
import org.ouracademy.ovejafiel.organization.domain.MemberVisibility;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.domain.SocialLinks;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.PersonDetails;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_children.FindOrganizationChildrenQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeResult;
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

@WebMvcTest(OrganizationController.class)
@ContextConfiguration(classes = OvejafielApplication.class)
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
  @MockitoBean GetMemberGrowthQueryHandler getMemberGrowthQueryHandler;
  @MockitoBean FindOrganizationSubtreeQueryHandler findOrganizationSubtreeQueryHandler;
  @MockitoBean AuthorizeQueryHandler authorizeQueryHandler;

  static final PersonDetails ANA_DETAILS =
      new PersonDetails("Ana", "Diaz", "999", 30, LocalDate.of(1995, 5, 17), true);

  String bearer() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  static FindOrganizationResult church(PartyId id) {
    return new FindOrganizationResult(
        id,
        "Church",
        "desc",
        "addr",
        "123",
        Organization.Type.CHURCH,
        null,
        null,
        null,
        MemberVisibility.PUBLIC,
        true,
        "slug",
        null,
        null);
  }

  @Test
  void createBuildsTheChurchCommandForTheSignedInUser() {
    var id = PartyId.generate();
    var command =
        new CreateChurchCommand(
            new OrganizationInfo(
                "Church",
                "desc",
                "addr",
                "123",
                new SocialLinks(null, "https://facebook.com/x", null, null, null)),
            new OrganizationInfo("Sede", null, "Av. 1", null, SocialLinks.NONE),
            List.of(
                new PositionSeed("Pastor", Set.of(PredefinedRoles.ADMIN), Set.of("events.write"))),
            new Creator("Ana", "Diaz", "Pastor"),
            "test-user",
            null,
            false);
    given(createChurchCommandHandler.handle(command))
        .willReturn(new CreateChurchResult(church(id), church(PartyId.generate())));

    assertThat(
            mvc.post()
                .uri("/v1/organizations")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Church","description":"desc","address":"addr","phoneNumber":"123",
                     "socialLinks":{"facebookUrl":"https://facebook.com/x","website":" "},
                     "location":{"name":"Sede","address":"Av. 1"},
                     "positions":[{"name":"Pastor","roleIds":["%s"],"permissions":["events.write"]}],
                     "creator":{"firstName":"Ana","lastName":"Diaz","positionName":"Pastor"}}
                    """
                        .formatted(PredefinedRoles.ADMIN.value())))
        .hasStatus(201)
        .bodyJson()
        .extractingPath("$.id")
        .isEqualTo(id.value().toString());
  }

  @Test
  void createWithoutPositionsNorPositionLevelsUsesEmptyOnes() {
    var id = PartyId.generate();
    var command =
        new CreateChurchCommand(
            new OrganizationInfo("Church", null, null, null, SocialLinks.NONE),
            new OrganizationInfo("Sede", null, null, null, SocialLinks.NONE),
            List.of(new PositionSeed("Hermano", Set.of(), Set.of())),
            new Creator("Ana", "Diaz", null),
            "test-user",
            null,
            false);
    given(createChurchCommandHandler.handle(command))
        .willReturn(new CreateChurchResult(church(id), church(PartyId.generate())));

    assertThat(
            mvc.post()
                .uri("/v1/organizations")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Church","location":{"name":"Sede"},
                     "positions":[{"name":"Hermano"}],
                     "creator":{"firstName":"Ana","lastName":"Diaz"}}
                    """))
        .hasStatus(201);

    var withoutPositions =
        new CreateChurchCommand(
            command.info(),
            command.location(),
            List.of(),
            command.creator(),
            "test-user",
            null,
            false);
    given(createChurchCommandHandler.handle(withoutPositions))
        .willReturn(new CreateChurchResult(church(id), church(PartyId.generate())));

    for (var positions : List.of("", "\"positions\":null,")) {
      assertThat(
              mvc.post()
                  .uri("/v1/organizations")
                  .header("Authorization", bearer())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {"name":"Church","location":{"name":"Sede"},%s
                       "creator":{"firstName":"Ana","lastName":"Diaz"}}
                      """
                          .formatted(positions)))
          .hasStatus(201);
    }
  }

  @Test
  void createWithoutALocationIsBadRequest() {
    assertThat(
            mvc.post()
                .uri("/v1/organizations")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Church\"}"))
        .hasStatus(400);
    verifyNoInteractions(createChurchCommandHandler);
  }

  @Test
  void createWithAnInvalidSocialLinkIsBadRequest() {
    assertThat(
            mvc.post()
                .uri("/v1/organizations")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Church","socialLinks":{"tiktokUrl":"tiktok.com/x"},
                     "location":{"name":"Sede"}}
                    """))
        .hasStatus(400);
  }

  @Test
  void readingANodeWithoutPermissionIsForbidden() {
    var id = PartyId.generate();
    given(authorizeQueryHandler.handle(new AuthorizeQuery("test-user", id, Permission.ORG_READ)))
        .willThrow(new AccessDeniedException("no"));

    assertThat(mvc.get().uri("/v1/organizations/" + id.value()).header("Authorization", bearer()))
        .hasStatus(403);
    verifyNoInteractions(findOrganizationByIdQueryHandler);
  }

  @Test
  void creatingAChildNeedsCreateChildOnTheParent() {
    var parentId = PartyId.generate();
    given(
            authorizeQueryHandler.handle(
                new AuthorizeQuery("test-user", parentId, Permission.ORG_CREATE_CHILD)))
        .willThrow(new AccessDeniedException("no"));

    assertThat(
            mvc.post()
                .uri("/v1/organizations/locations/childs")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Coro\",\"type\":\"GROUP\",\"parentId\":\""
                        + parentId.value()
                        + "\"}"))
        .hasStatus(403);
    verifyNoInteractions(createLocationChildCommandHandler);
  }

  @Test
  void deletingNeedsOrgDelete() {
    var id = PartyId.generate();
    given(authorizeQueryHandler.handle(new AuthorizeQuery("test-user", id, Permission.ORG_DELETE)))
        .willThrow(new AccessDeniedException("no"));

    assertThat(
            mvc.delete().uri("/v1/organizations/" + id.value()).header("Authorization", bearer()))
        .hasStatus(403);
    verifyNoInteractions(deleteOrganizationCommandHandler);
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
  void createLocationWithoutAParentIsBadRequest() {
    assertThat(
            mvc.post()
                .uri("/v1/organizations/locations")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sede\"}"))
        .hasStatus(400);
  }

  @Test
  void updateOfAnUnknownLocationIsNotFound() {
    given(updateLocationCommandHandler.handle(any())).willReturn(Optional.empty());

    assertThat(
            mvc.put()
                .uri("/v1/organizations/locations/" + PartyId.generate().value())
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sede\"}"))
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
                .content("{\"name\":\"Youth\",\"type\":\"MINISTRY\"}"))
        .hasStatus(404);
  }

  @Test
  void updateRejectsAChurchOrLocationType() {
    assertThat(
            mvc.put()
                .uri("/v1/organizations/locations/childs/" + PartyId.generate().value())
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Church\",\"type\":\"CHURCH\"}"))
        .hasStatus(400);
  }

  @Test
  void growthReturnsTheCurrentCountsAndTheMonthsAndClampsTheRange() {
    var id = PartyId.generate();
    var below = PartyId.generate();
    given(findOrganizationSubtreeQueryHandler.handle(any()))
        .willReturn(new FindOrganizationSubtreeResult(List.of(id, below)));
    given(getMemberGrowthQueryHandler.handle(any()))
        .willReturn(
            new MemberGrowthResult(
                new MemberGrowthResult.StateCounts(4, 3, 10),
                List.of(new MemberGrowthResult.Month("2026-10", 5, 2, 1, 1, 17, 10))));

    var response =
        assertThat(
                mvc.get()
                    .uri("/v1/organizations/" + id.value() + "/growth?months=999")
                    .header("Authorization", bearer()))
            .hasStatusOk()
            .bodyJson();
    response.extractingPath("$.current.members").isEqualTo(10);
    response.extractingPath("$.current.total").isEqualTo(17);
    response.extractingPath("$.months[0].month").isEqualTo("2026-10");
    response.extractingPath("$.months[0].membersAtEnd").isEqualTo(10);

    org.mockito.Mockito.verify(getMemberGrowthQueryHandler)
        .handle(new GetMemberGrowthQuery(List.of(id, below), 36));
    org.mockito.Mockito.verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", id, Permission.MEMBERS_READ));
  }

  @Test
  void growthNeedsMembersRead() {
    org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("no"))
        .when(authorizeQueryHandler)
        .handle(any());

    assertThat(
            mvc.get()
                .uri("/v1/organizations/" + PartyId.generate().value() + "/growth")
                .header("Authorization", bearer()))
        .hasStatus(403);
    org.mockito.Mockito.verifyNoInteractions(getMemberGrowthQueryHandler);
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
}
