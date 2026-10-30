package org.ouracademy.ovejafiel.directory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.directory.query.DirectoryNodeResult;
import org.ouracademy.ovejafiel.directory.query.get_church_directory.GetChurchDirectoryQuery;
import org.ouracademy.ovejafiel.directory.query.get_church_directory.GetChurchDirectoryQueryHandler;
import org.ouracademy.ovejafiel.directory.query.get_church_directory.GetChurchDirectoryResult;
import org.ouracademy.ovejafiel.directory.query.get_directory_entry.GetDirectoryEntryQuery;
import org.ouracademy.ovejafiel.directory.query.get_directory_entry.GetDirectoryEntryQueryHandler;
import org.ouracademy.ovejafiel.directory.query.get_directory_entry.GetDirectoryEntryResult;
import org.ouracademy.ovejafiel.directory.query.get_my_nodes.GetMyNodesQueryHandler;
import org.ouracademy.ovejafiel.directory.query.get_public_page.GetPublicPageQuery;
import org.ouracademy.ovejafiel.directory.query.get_public_page.GetPublicPageQueryHandler;
import org.ouracademy.ovejafiel.directory.query.get_public_page.GetPublicPageResult;
import org.ouracademy.ovejafiel.organization.domain.MemberVisibility;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.domain.SocialLinks;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(DirectoryController.class)
@ContextConfiguration(classes = OvejafielApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class DirectoryControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean GetChurchDirectoryQueryHandler getChurchDirectoryQueryHandler;
  @MockitoBean GetDirectoryEntryQueryHandler getDirectoryEntryQueryHandler;
  @MockitoBean GetPublicPageQueryHandler getPublicPageQueryHandler;
  @MockitoBean GetMyNodesQueryHandler getMyNodesQueryHandler;

  PartyId church = PartyId.generate();

  String bearer() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  DirectoryNodeResult node(String name) {
    return new DirectoryNodeResult(
        PartyId.generate(),
        name,
        Organization.Type.GROUP,
        church,
        "desc",
        "Sábados",
        MemberVisibility.MEMBERS,
        List.of("Ana Ruiz"));
  }

  @Test
  void theDirectoryListsTheNodesForTheSignedInUser() {
    given(getChurchDirectoryQueryHandler.handle(any()))
        .willReturn(new GetChurchDirectoryResult(List.of(node("Jóvenes"))));

    var body =
        assertThat(
                mvc.get()
                    .uri("/v1/organizations/" + church.value() + "/directory")
                    .header("Authorization", bearer()))
            .hasStatusOk()
            .bodyJson();
    body.extractingPath("$[0].name").isEqualTo("Jóvenes");
    body.extractingPath("$[0].memberVisibility").isEqualTo("MEMBERS");
    body.extractingPath("$[0].team[0]").isEqualTo("Ana Ruiz");
    verify(getChurchDirectoryQueryHandler).handle(new GetChurchDirectoryQuery("test-user", church));
  }

  @Test
  void theDirectoryNeedsASessionAndMembershipOfTheChurch() {
    assertThat(mvc.get().uri("/v1/organizations/" + church.value() + "/directory")).hasStatus(401);

    given(getChurchDirectoryQueryHandler.handle(any())).willThrow(new AccessDeniedException("no"));
    assertThat(
            mvc.get()
                .uri("/v1/organizations/" + church.value() + "/directory")
                .header("Authorization", bearer()))
        .hasStatus(403);
  }

  @Test
  void anEntryTellsWhetherThePeopleAreVisible() {
    var hidden = new GetDirectoryEntryResult(node("Jóvenes"), false, List.of(), List.of());
    given(getDirectoryEntryQueryHandler.handle(any())).willReturn(hidden);
    var id = PartyId.generate();

    var body =
        assertThat(
                mvc.get()
                    .uri("/v1/organizations/" + id.value() + "/directory-entry")
                    .header("Authorization", bearer()))
            .hasStatusOk()
            .bodyJson();
    body.extractingPath("$.membersVisible").isEqualTo(false);
    body.extractingPath("$.node.name").isEqualTo("Jóvenes");
    verify(getDirectoryEntryQueryHandler).handle(new GetDirectoryEntryQuery("test-user", id));
  }

  @Test
  void thePublicPageNeedsNoSession() {
    given(getPublicPageQueryHandler.handle(new GetPublicPageQuery("abc")))
        .willReturn(
            Optional.of(
                new GetPublicPageResult(
                    "Iglesia",
                    Organization.Type.CHURCH,
                    "desc",
                    "Domingos",
                    "Av. 1",
                    "999",
                    SocialLinks.NONE,
                    List.of("Ana Ruiz"),
                    null,
                    List.of(new GetPublicPageResult.PositionPeople("Pastor", List.of("Luis Paz"))),
                    new GetPublicPageResult.OrgChartNode(
                        "Iglesia", Organization.Type.CHURCH, "abc", List.of(), List.of()))));

    var body = assertThat(mvc.get().uri("/v1/public/organizations/abc")).hasStatusOk().bodyJson();
    body.extractingPath("$.name").isEqualTo("Iglesia");
    body.extractingPath("$.positions[0].people[0]").isEqualTo("Luis Paz");
    body.extractingPath("$.orgChart.slug").isEqualTo("abc");
  }

  @Test
  void aPageThatIsNotPublicOrDoesNotExistIsNotFound() {
    given(getPublicPageQueryHandler.handle(any())).willReturn(Optional.empty());

    assertThat(mvc.get().uri("/v1/public/organizations/nope")).hasStatus(404);
  }
}
