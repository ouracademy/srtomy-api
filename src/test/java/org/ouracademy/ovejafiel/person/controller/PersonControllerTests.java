package org.ouracademy.ovejafiel.person.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.ouracademy.ovejafiel.person.PersonFixtures.personResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.accountability.command.set_node_membership.SetNodeMembershipCommand;
import org.ouracademy.ovejafiel.accountability.command.set_node_membership.SetNodeMembershipCommandHandler;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes;
import org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member.OrganizationsOfMemberResult;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.find_organization_ancestors.FindOrganizationAncestorsQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_ancestors.FindOrganizationAncestorsResult;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.command.PersonInfo;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonCommand;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonCommandHandler;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonResult;
import org.ouracademy.ovejafiel.person.command.delete_person.DeletePersonCommand;
import org.ouracademy.ovejafiel.person.command.delete_person.DeletePersonCommandHandler;
import org.ouracademy.ovejafiel.person.command.delete_person.DeletePersonResult;
import org.ouracademy.ovejafiel.person.command.delete_person.DeletePersonResult.Outcome;
import org.ouracademy.ovejafiel.person.command.restore_person.RestorePersonCommand;
import org.ouracademy.ovejafiel.person.command.restore_person.RestorePersonCommandHandler;
import org.ouracademy.ovejafiel.person.command.restore_person.RestorePersonResult;
import org.ouracademy.ovejafiel.person.command.update_person.UpdatePersonCommand;
import org.ouracademy.ovejafiel.person.command.update_person.UpdatePersonCommandHandler;
import org.ouracademy.ovejafiel.person.command.update_person.UpdatePersonResult;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.person.query.find_person_by_id.FindPersonByIdQuery;
import org.ouracademy.ovejafiel.person.query.find_person_by_id.FindPersonByIdQueryHandler;
import org.ouracademy.ovejafiel.person.query.find_person_matches.FindPersonMatchesQuery;
import org.ouracademy.ovejafiel.person.query.find_person_matches.FindPersonMatchesQueryHandler;
import org.ouracademy.ovejafiel.person.query.find_person_matches.FindPersonMatchesResult;
import org.ouracademy.ovejafiel.person.query.find_person_matches.FindPersonMatchesResult.Match;
import org.ouracademy.ovejafiel.person.query.get_person_memberships.GetPersonMembershipsQueryHandler;
import org.ouracademy.ovejafiel.person.query.get_person_memberships.PersonMembershipResult;
import org.ouracademy.ovejafiel.person.query.search_people.SearchPeopleQuery;
import org.ouracademy.ovejafiel.person.query.search_people.SearchPeopleQueryHandler;
import org.ouracademy.ovejafiel.person.query.search_people.SearchPeopleResult;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(PersonController.class)
@ContextConfiguration(classes = OvejafielApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class PersonControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean SearchPeopleQueryHandler searchPeopleQueryHandler;
  @MockitoBean FindPersonByIdQueryHandler findPersonByIdQueryHandler;
  @MockitoBean CreatePersonCommandHandler createPersonCommandHandler;
  @MockitoBean UpdatePersonCommandHandler updatePersonCommandHandler;
  @MockitoBean FindOrganizationsOfMemberQueryHandler findOrganizationsOfMemberQueryHandler;
  @MockitoBean AuthorizeQueryHandler authorizeQueryHandler;
  @MockitoBean DeletePersonCommandHandler deletePersonCommandHandler;
  @MockitoBean RestorePersonCommandHandler restorePersonCommandHandler;
  @MockitoBean FindPersonMatchesQueryHandler findPersonMatchesQueryHandler;
  @MockitoBean FindOrganizationSubtreeQueryHandler findOrganizationSubtreeQueryHandler;
  @MockitoBean FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  @MockitoBean FindOrganizationAncestorsQueryHandler findOrganizationAncestorsQueryHandler;
  @MockitoBean SetNodeMembershipCommandHandler setNodeMembershipCommandHandler;
  @MockitoBean GetPersonMembershipsQueryHandler getPersonMembershipsQueryHandler;

  @BeforeEach
  void everyPersonBelongsToSomeNode() {
    memberOf(PartyId.generate());
  }

  private String bearerHeader() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  @Test
  void findAllForAnUnknownOrganizationReturnsAnEmptyPage() {
    var organizationId = UUID.randomUUID();
    given(searchPeopleQueryHandler.handle(any())).willReturn(new SearchPeopleResult(Page.empty()));

    assertThat(
            mvc.get()
                .uri("/v1/people?organizationId=" + organizationId)
                .header("Authorization", bearerHeader()))
        .hasStatusOk()
        .bodyText()
        .contains("\"totalElements\":0");

    var query = ArgumentCaptor.forClass(SearchPeopleQuery.class);
    verify(searchPeopleQueryHandler).handle(query.capture());
    assertThat(query.getValue().organizationId()).isEqualTo(new PartyId(organizationId));
    assertThat(query.getValue().searchText()).isNull();
  }

  @Test
  void findAllPassesTheSearchTextAndReturnsThePeople() {
    var organizationId = UUID.randomUUID();
    var ana = personResult(PartyId.generate(), "Ana", "Diaz");
    given(searchPeopleQueryHandler.handle(any()))
        .willReturn(new SearchPeopleResult(new PageImpl<>(List.of(ana))));

    var response =
        assertThat(
                mvc.get()
                    .uri("/v1/people?organizationId=" + organizationId + "&searchText=an")
                    .header("Authorization", bearerHeader()))
            .hasStatusOk()
            .bodyJson();
    response.extractingPath("$.content[0].firstName").isEqualTo("Ana");
    response.extractingPath("$.content[0].fullName").isEqualTo("Ana Diaz");

    var query = ArgumentCaptor.forClass(SearchPeopleQuery.class);
    verify(searchPeopleQueryHandler).handle(query.capture());
    assertThat(query.getValue().searchText()).isEqualTo("an");
  }

  @Test
  void findAllWithoutOrganizationIdIsABadRequest() {
    assertThat(mvc.get().uri("/v1/people").header("Authorization", bearerHeader()))
        .hasStatus(HttpStatus.BAD_REQUEST);
    verifyNoInteractions(searchPeopleQueryHandler);
  }

  @Test
  void findByIdReturnsThePersonFields() {
    var id = PartyId.generate();
    given(findPersonByIdQueryHandler.handle(new FindPersonByIdQuery(id)))
        .willReturn(Optional.of(personResult(id, "Ana", "Diaz")));

    var response =
        assertThat(
                mvc.get().uri("/v1/people/" + id.value()).header("Authorization", bearerHeader()))
            .hasStatusOk()
            .bodyJson();
    response.extractingPath("$.id").isEqualTo(id.value().toString());
    response.extractingPath("$.name").isEqualTo("Ana Diaz");
    response.extractingPath("$.lastName").isEqualTo("Diaz");
    response.extractingPath("$.birthdate").isEqualTo("1995-05-17");
  }

  @Test
  void findByIdOfAMissingPersonIsNotFound() {
    var id = PartyId.generate();
    given(findPersonByIdQueryHandler.handle(new FindPersonByIdQuery(id)))
        .willReturn(Optional.empty());

    assertThat(mvc.get().uri("/v1/people/" + id.value()).header("Authorization", bearerHeader()))
        .hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void createTranslatesTheRequestIntoACommandAndReturnsTheNewId() {
    var organizationId = UUID.randomUUID();
    var roleId = UUID.randomUUID();
    var newId = PartyId.generate();
    var expected =
        new CreatePersonCommand(
            "test-user",
            new PersonInfo("Ana", "Diaz", "999", null, 30, LocalDate.of(1995, 5, 17), true),
            new PartyId(organizationId),
            Set.of(new AccountabilityTypeId(roleId)));
    given(createPersonCommandHandler.handle(expected)).willReturn(new CreatePersonResult(newId));

    assertThat(
            mvc.post()
                .uri("/v1/people")
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(organizationId, roleId, "Ana")))
        .hasStatus(HttpStatus.CREATED)
        .bodyText()
        .isEqualTo("\"" + newId.value() + "\"");
  }

  @Test
  void createWithoutAFirstNameIsABadRequest() {
    assertThat(
            mvc.post()
                .uri("/v1/people")
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(UUID.randomUUID(), UUID.randomUUID(), "")))
        .hasStatus(HttpStatus.BAD_REQUEST);
    verifyNoInteractions(createPersonCommandHandler);
  }

  @Test
  void editReturnsTheIdOfTheUpdatedPerson() {
    var id = PartyId.generate();
    var organizationId = UUID.randomUUID();
    var roleId = UUID.randomUUID();
    memberOf(new PartyId(organizationId));
    var expected =
        new UpdatePersonCommand(
            "test-user",
            id,
            new PersonInfo("Ana", "Diaz", "999", null, 30, LocalDate.of(1995, 5, 17), true),
            new PartyId(organizationId),
            Set.of(new AccountabilityTypeId(roleId)));
    given(updatePersonCommandHandler.handle(expected))
        .willReturn(Optional.of(new UpdatePersonResult(id)));

    assertThat(
            mvc.put()
                .uri("/v1/people/" + id.value())
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(organizationId, roleId, "Ana")))
        .hasStatusOk()
        .bodyText()
        .isEqualTo("\"" + id.value() + "\"");
  }

  @Test
  void editOfAMissingPersonIsNotFound() {
    var organizationId = UUID.randomUUID();
    memberOf(new PartyId(organizationId));
    given(updatePersonCommandHandler.handle(any())).willReturn(Optional.empty());

    assertThat(
            mvc.put()
                .uri("/v1/people/" + UUID.randomUUID())
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(organizationId, UUID.randomUUID(), "Ana")))
        .hasStatus(HttpStatus.NOT_FOUND);
  }

  private static String body(UUID organizationId, UUID roleId, String firstName) {
    return """
        {
          "firstName": "%s",
          "lastName": "Diaz",
          "phoneNumber": "999",
          "age": 30,
          "birthdate": "1995-05-17",
          "hasBeenBaptized": true,
          "positionIds": ["%s"],
          "organizationId": "%s"
        }
        """
        .formatted(firstName, roleId, organizationId);
  }

  @Test
  void editingSomeoneOutsideTheGivenNodeIsForbidden() {
    memberOf(PartyId.generate());

    assertThat(
            mvc.put()
                .uri("/v1/people/" + UUID.randomUUID())
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(UUID.randomUUID(), UUID.randomUUID(), "Ana")))
        .hasStatus(HttpStatus.FORBIDDEN);
    verifyNoInteractions(updatePersonCommandHandler);
  }

  @Test
  void readingAPersonNeedsMembersReadOnOneOfTheirNodes() {
    var organizationId = PartyId.generate();
    memberOf(organizationId);
    given(
            authorizeQueryHandler.handle(
                new AuthorizeQuery("test-user", List.of(organizationId), Permission.MEMBERS_READ)))
        .willThrow(new AccessDeniedException("no"));

    assertThat(
            mvc.get()
                .uri("/v1/people/" + UUID.randomUUID())
                .header("Authorization", bearerHeader()))
        .hasStatus(HttpStatus.FORBIDDEN);
    verifyNoInteractions(findPersonByIdQueryHandler);
  }

  @Test
  void deleteNeedsMembersDeleteOnTheNodeAndAnswersNoContent() {
    var personId = UUID.randomUUID();
    var node = PartyId.generate();
    memberOf(node);
    subtreeOf(node);
    given(deletePersonCommandHandler.handle(any()))
        .willReturn(new DeletePersonResult(Outcome.REMOVED_FROM_NODE));

    assertThat(
            mvc.delete()
                .uri("/v1/people/" + personId + "?organizationId=" + node.value())
                .header("Authorization", bearerHeader()))
        .hasStatus(HttpStatus.NO_CONTENT);

    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", node, Permission.MEMBERS_DELETE));
    verify(deletePersonCommandHandler)
        .handle(new DeletePersonCommand(new PartyId(personId), node, false));
  }

  @Test
  void deleteOfSomeoneOutsideTheNodeIsForbidden() {
    var node = PartyId.generate();
    memberOf(PartyId.generate());
    subtreeOf(node);

    assertThat(
            mvc.delete()
                .uri("/v1/people/" + UUID.randomUUID() + "?organizationId=" + node.value())
                .header("Authorization", bearerHeader()))
        .hasStatus(HttpStatus.FORBIDDEN);
    verifyNoInteractions(deletePersonCommandHandler);
  }

  @Test
  void deleteOfAPersonWithAnAccountAsksForConfirmationWith409() {
    var node = PartyId.generate();
    memberOf(node);
    subtreeOf(node);
    given(deletePersonCommandHandler.handle(any()))
        .willReturn(new DeletePersonResult(Outcome.REQUIRES_USER_UNLINK));

    var response =
        assertThat(
                mvc.delete()
                    .uri("/v1/people/" + UUID.randomUUID() + "?organizationId=" + node.value())
                    .header("Authorization", bearerHeader()))
            .hasStatus(HttpStatus.CONFLICT)
            .bodyJson();
    response.extractingPath("$.code").isEqualTo("PERSON_HAS_ACCOUNT");
  }

  @Test
  void deleteWithConfirmUnlinkUserPassesTheConfirmation() {
    var personId = UUID.randomUUID();
    var node = PartyId.generate();
    memberOf(node);
    subtreeOf(node);
    given(deletePersonCommandHandler.handle(any()))
        .willReturn(new DeletePersonResult(Outcome.DELETED));

    assertThat(
            mvc.delete()
                .uri(
                    "/v1/people/"
                        + personId
                        + "?organizationId="
                        + node.value()
                        + "&confirmUnlinkUser=true")
                .header("Authorization", bearerHeader()))
        .hasStatus(HttpStatus.NO_CONTENT);
    verify(deletePersonCommandHandler)
        .handle(new DeletePersonCommand(new PartyId(personId), node, true));
  }

  @Test
  void deleteOfAMissingPersonIsNotFound() {
    var node = PartyId.generate();
    memberOf(node);
    subtreeOf(node);
    given(deletePersonCommandHandler.handle(any()))
        .willReturn(new DeletePersonResult(Outcome.NOT_FOUND));

    assertThat(
            mvc.delete()
                .uri("/v1/people/" + UUID.randomUUID() + "?organizationId=" + node.value())
                .header("Authorization", bearerHeader()))
        .hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void deleteWithoutPermissionIsForbiddenAndDoesNothing() {
    var node = PartyId.generate();
    org.mockito.Mockito.doThrow(new AccessDeniedException("no"))
        .when(authorizeQueryHandler)
        .handle(any());

    assertThat(
            mvc.delete()
                .uri("/v1/people/" + UUID.randomUUID() + "?organizationId=" + node.value())
                .header("Authorization", bearerHeader()))
        .hasStatus(HttpStatus.FORBIDDEN);
    verifyNoInteractions(deletePersonCommandHandler);
  }

  @Test
  void matchesReturnsActiveAndRemovedPeopleForTheRegistrationForm() {
    var node = PartyId.generate();
    var activeId = PartyId.generate();
    var removedId = PartyId.generate();
    var removedAt = java.time.LocalDateTime.of(2026, 3, 4, 10, 0);
    given(findPersonMatchesQueryHandler.handle(any()))
        .willReturn(
            new FindPersonMatchesResult(
                List.of(
                    new Match(activeId, "Ana Diaz", "ana@x.org", "999", true, null),
                    new Match(removedId, "Ana Diaz", null, null, false, removedAt))));

    var response =
        assertThat(
                mvc.get()
                    .uri(
                        "/v1/people/matches?organizationId="
                            + node.value()
                            + "&searchText=Ana&email=ana@x.org")
                    .header("Authorization", bearerHeader()))
            .hasStatusOk()
            .bodyJson();
    response.extractingPath("$[0].status").isEqualTo("ACTIVE");
    response.extractingPath("$[0].id").isEqualTo(activeId.value().toString());
    response.extractingPath("$[1].status").isEqualTo("REMOVED");
    response.extractingPath("$[1].removedTime").isEqualTo("2026-03-04T10:00:00");
    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", node, Permission.MEMBERS_WRITE));
    verify(findPersonMatchesQueryHandler)
        .handle(new FindPersonMatchesQuery(node, "Ana", "ana@x.org"));
  }

  @Test
  void restoreBringsThePersonBackToTheNode() {
    var personId = UUID.randomUUID();
    var node = PartyId.generate();
    given(restorePersonCommandHandler.handle(any()))
        .willReturn(new RestorePersonResult(new PartyId(personId)));

    assertThat(
            mvc.post()
                .uri("/v1/people/" + personId + "/restore?organizationId=" + node.value())
                .header("Authorization", bearerHeader()))
        .hasStatusOk()
        .bodyText()
        .contains(personId.toString());

    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", node, Permission.MEMBERS_WRITE));
    verify(restorePersonCommandHandler)
        .handle(new RestorePersonCommand(new PartyId(personId), node));
  }

  @Test
  void findAllShowsTheStateOfEachPersonInTheNodeAndPassesTheStateFilter() {
    var organizationId = UUID.randomUUID();
    var anaId = PartyId.generate();
    var ana = personResult(anaId, "Ana", "Diaz");
    var luis = personResult(PartyId.generate(), "Luis", "Paz");
    given(searchPeopleQueryHandler.handle(any()))
        .willReturn(
            new SearchPeopleResult(
                new PageImpl<>(List.of(ana, luis)),
                java.util.Map.of(anaId, MemberState.MEMBERSHIP_IN_PROGRESS)));

    var response =
        assertThat(
                mvc.get()
                    .uri(
                        "/v1/people?organizationId="
                            + organizationId
                            + "&memberState=MEMBERSHIP_IN_PROGRESS")
                    .header("Authorization", bearerHeader()))
            .hasStatusOk()
            .bodyJson();
    response.extractingPath("$.content[0].memberState").isEqualTo("MEMBERSHIP_IN_PROGRESS");
    // Luis is only in a node below this one, so he has no state here.
    response.extractingPath("$.content[1].memberState").isNull();

    var query = ArgumentCaptor.forClass(SearchPeopleQuery.class);
    verify(searchPeopleQueryHandler).handle(query.capture());
    assertThat(query.getValue().state()).isEqualTo(MemberState.MEMBERSHIP_IN_PROGRESS);
  }

  @Test
  void membershipsListsOnlyTheNodesTheCallerCanRead() {
    var personId = UUID.randomUUID();
    var visible = PartyId.generate();
    var hidden = PartyId.generate();
    memberOf(visible);
    given(getPersonMembershipsQueryHandler.handle(any()))
        .willReturn(
            List.of(
                new PersonMembershipResult(
                    visible,
                    "Jóvenes",
                    Organization.Type.GROUP,
                    MemberState.MEMBERSHIP_IN_PROGRESS,
                    List.of("Tesorero")),
                new PersonMembershipResult(
                    hidden,
                    "Alabanza",
                    Organization.Type.MINISTRY,
                    MemberState.MEMBER,
                    List.of())));
    org.mockito.Mockito.doThrow(new AccessDeniedException("no"))
        .when(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", hidden, Permission.MEMBERS_READ));

    var response =
        assertThat(
                mvc.get()
                    .uri("/v1/people/" + personId + "/memberships")
                    .header("Authorization", bearerHeader()))
            .hasStatusOk()
            .bodyJson();
    response.extractingPath("$.length()").isEqualTo(1);
    response.extractingPath("$[0].organizationName").isEqualTo("Jóvenes");
    response.extractingPath("$[0].organizationType").isEqualTo("GROUP");
    response.extractingPath("$[0].memberState").isEqualTo("MEMBERSHIP_IN_PROGRESS");
    response.extractingPath("$[0].positions[0]").isEqualTo("Tesorero");
  }

  @Test
  void putMembershipPutsAnExistingPersonInTheNodeAndItsAncestorsWithTheStateAsked() {
    var personId = PartyId.generate();
    var church = PartyId.generate();
    var group = PartyId.generate();
    memberOf(church);
    aPersonExists(personId);
    nodeIs(group, church);
    given(findOrganizationAncestorsQueryHandler.handle(any()))
        .willReturn(new FindOrganizationAncestorsResult(List.of(church)));

    assertThat(
            mvc.put()
                .uri("/v1/people/" + personId.value() + "/memberships/" + group.value())
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberState\":\"MEMBERSHIP_IN_PROGRESS\"}"))
        .hasStatus(HttpStatus.NO_CONTENT);

    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", group, Permission.MEMBERS_WRITE));
    verify(setNodeMembershipCommandHandler)
        .handle(
            new SetNodeMembershipCommand(
                personId,
                group,
                List.of(church),
                SystemAccountabilityTypes.MEMBERSHIP_IN_PROGRESS));
  }

  @Test
  void putMembershipOfSomeoneFromAnotherChurchIsForbidden() {
    var personId = PartyId.generate();
    var church = PartyId.generate();
    var group = PartyId.generate();
    memberOf(PartyId.generate());
    aPersonExists(personId);
    nodeIs(group, church);

    assertThat(
            mvc.put()
                .uri("/v1/people/" + personId.value() + "/memberships/" + group.value())
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberState\":\"MEMBER\"}"))
        .hasStatus(HttpStatus.FORBIDDEN);
    verifyNoInteractions(setNodeMembershipCommandHandler);
  }

  @Test
  void putMembershipWithoutAStateIsABadRequest() {
    assertThat(
            mvc.put()
                .uri("/v1/people/" + UUID.randomUUID() + "/memberships/" + UUID.randomUUID())
                .header("Authorization", bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .hasStatus(HttpStatus.BAD_REQUEST);
    verifyNoInteractions(setNodeMembershipCommandHandler);
  }

  private void aPersonExists(PartyId id) {
    given(findPersonByIdQueryHandler.handle(new FindPersonByIdQuery(id)))
        .willReturn(Optional.of(personResult(id, "Ana", "Diaz")));
  }

  /** {@code node} lives in {@code church}, whose tree is just itself. */
  private void nodeIs(PartyId node, PartyId church) {
    given(findChurchOfOrganizationQueryHandler.handle(any()))
        .willReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    subtreeOf(church, node);
  }

  private void subtreeOf(PartyId... nodes) {
    given(findOrganizationSubtreeQueryHandler.handle(any()))
        .willReturn(new FindOrganizationSubtreeResult(List.of(nodes)));
  }

  private void memberOf(PartyId organizationId) {
    given(findOrganizationsOfMemberQueryHandler.handle(any()))
        .willReturn(new OrganizationsOfMemberResult(List.of(organizationId)));
  }
}
