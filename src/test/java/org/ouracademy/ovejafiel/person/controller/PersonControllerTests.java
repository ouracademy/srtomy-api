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
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member.OrganizationsOfMemberResult;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.command.PersonInfo;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonCommand;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonCommandHandler;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonResult;
import org.ouracademy.ovejafiel.person.command.update_person.UpdatePersonCommand;
import org.ouracademy.ovejafiel.person.command.update_person.UpdatePersonCommandHandler;
import org.ouracademy.ovejafiel.person.command.update_person.UpdatePersonResult;
import org.ouracademy.ovejafiel.person.query.find_person_by_id.FindPersonByIdQuery;
import org.ouracademy.ovejafiel.person.query.find_person_by_id.FindPersonByIdQueryHandler;
import org.ouracademy.ovejafiel.person.query.search_people.SearchPeopleQuery;
import org.ouracademy.ovejafiel.person.query.search_people.SearchPeopleQueryHandler;
import org.ouracademy.ovejafiel.person.query.search_people.SearchPeopleResult;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
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

  private void memberOf(PartyId organizationId) {
    given(findOrganizationsOfMemberQueryHandler.handle(any()))
        .willReturn(new OrganizationsOfMemberResult(List.of(organizationId)));
  }
}
