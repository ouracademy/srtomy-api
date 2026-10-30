package org.ouracademy.ovejafiel.person;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.ouracademy.ovejafiel.user.command.sync_current_user.SyncCurrentUserCommand;
import org.ouracademy.ovejafiel.user.command.sync_current_user.SyncCurrentUserCommandHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * A person has a state (visitor, attendee, member) in each node they belong to: attendee of the
 * youth group while a member of the church. These tests go over the real schema: adding an existing
 * person to a group, listing and filtering by state, the history of changes and the growth numbers
 * built from it.
 */
@SpringBootTest(classes = OvejafielApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class MembershipIntegrationTests {

  static final ObjectMapper JSON = new ObjectMapper();

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  @Autowired JdbcTemplate jdbc;

  RestTestClient client;
  String alice;
  String bob;
  String churchId;
  String location;
  String youth;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port + "/v1").build();
    alice = signUp("alice");
    bob = signUp("bob");
    var created =
        read(
            body(
                alice,
                HttpMethod.POST,
                "/organizations",
                """
                {"name":"Iglesia","location":{"name":"Sede"},
                 "positions":[{"name":"Pastor","roleIds":["%s"]}],
                 "creator":{"firstName":"Ana","lastName":"Diaz","positionName":"Pastor"}}
                """
                    .formatted(PredefinedRoles.ADMIN.value())));
    churchId = created.get("id").asText();
    location = json(alice, "/organizations/" + churchId + "/childs").get(0).get("id").asText();
    youth = createGroup("Jóvenes", "GROUP");
  }

  /**
   * One walk through the whole feature, to keep the suite fast: the queries and the schema are what
   * only a real database can check; the rules have unit tests.
   */
  @Test
  void aPersonIsAnAttendeeOfAGroupWhileAMemberOfTheChurchAndTheChangesAreKept() {
    var lucia = createPerson("Lucía", location, "MEMBER");
    var pedro = createPerson("Pedro", location, "VISITOR");

    // Adding to a group: the state there, the church knows them too, with no extra access.
    put(alice, membership(lucia, youth), state("MEMBERSHIP_IN_PROGRESS"));
    put(alice, membership(pedro, youth), state("VISITOR"));
    assertThat(stateIn(lucia, youth)).isEqualTo("MEMBERSHIP_IN_PROGRESS");
    assertThat(stateIn(lucia, location)).isEqualTo("MEMBER");
    assertThat(stateIn(lucia, churchId)).isEqualTo("MEMBER");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from accountability a join accountability_type t on t.id = a.type"
                    + " where a.child_id = ?::uuid and a.parent_id = ?::uuid and t.name = 'IS_PART_OF'",
                Integer.class,
                lucia,
                churchId))
        .isZero();
    assertThat(
            json(alice, "/people/" + lucia + "/memberships").findValuesAsText("organizationName"))
        .containsExactlyInAnyOrder("Iglesia", "Sede", "Jóvenes");

    // The list filters by the state in the node being listed, and by the name on top of it.
    assertThat(namesIn(youth, "MEMBERSHIP_IN_PROGRESS")).containsExactly("Lucía Prueba");
    assertThat(namesIn(youth, "VISITOR")).containsExactly("Pedro Prueba");
    assertThat(namesIn(churchId, "MEMBER")).contains("Lucía Prueba").doesNotContain("Pedro Prueba");
    assertThat(
            json(alice, "/people?organizationId=" + youth + "&memberState=VISITOR&searchText=ped")
                .get("content"))
        .hasSize(1);

    // The group form no longer replaces its people.
    body(
        alice,
        HttpMethod.PUT,
        "/organizations/locations/childs/" + youth,
        "{\"name\":\"Jóvenes!\",\"type\":\"GROUP\"}");
    assertThat(namesIn(youth, null)).containsExactlyInAnyOrder("Lucía Prueba", "Pedro Prueba");

    // Who is not in the church, or cannot write members, cannot be added; a position holder stays
    // a member.
    assertThat(status(bob, HttpMethod.PUT, membership(lucia, youth), state("MEMBER")))
        .isEqualTo(403);
    var pastorId =
        jdbc.queryForObject(
            "select person_id::text from user_person where church_id = ?::uuid",
            String.class,
            churchId);
    assertThat(status(alice, HttpMethod.PUT, membership(pastorId, location), state("VISITOR")))
        .isEqualTo(400);

    // Every change is kept, and feeds the growth numbers.
    put(alice, membership(lucia, youth), state("MEMBER"));
    assertThat(
            jdbc.queryForList(
                "select from_state, to_state from member_state_event where person_id = ?::uuid"
                    + " and organization_id = ?::uuid order by occurred_at, id",
                lucia,
                youth))
        .extracting(row -> row.get("from_state") + ">" + row.get("to_state"))
        .containsExactly("null>MEMBERSHIP_IN_PROGRESS", "MEMBERSHIP_IN_PROGRESS>MEMBER");
    var growth = json(alice, "/organizations/" + youth + "/growth?months=2");
    assertThat(growth.get("current").get("members").asInt()).isEqualTo(1);
    assertThat(growth.get("current").get("visitors").asInt()).isEqualTo(1);
    var thisMonth = growth.get("months").get(1);
    assertThat(thisMonth.get("joined").asInt()).isEqualTo(2);
    assertThat(thisMonth.get("becameMember").asInt()).isEqualTo(1);
    assertThat(thisMonth.get("peopleAtEnd").asInt()).isEqualTo(2);
    assertThat(growth.get("months").get(0).get("peopleAtEnd").asInt()).isZero();

    // Leaving the group is kept too, and she is still in the church.
    assertThat(
            status(
                alice,
                HttpMethod.DELETE,
                "/people/%s?organizationId=%s&confirmUnlinkUser=false".formatted(lucia, youth)))
        .isEqualTo(204);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from member_state_event where person_id = ?::uuid"
                    + " and organization_id = ?::uuid and from_state = 'MEMBER' and to_state is null",
                Integer.class,
                lucia,
                youth))
        .isEqualTo(1);
    assertThat(
            json(alice, "/organizations/" + youth + "/growth?months=1")
                .get("months")
                .get(0)
                .get("left")
                .asInt())
        .isEqualTo(1);
    assertThat(namesIn(churchId, null)).contains("Lucía Prueba");
  }

  // --- helpers

  static String membership(String personId, String organizationId) {
    return "/people/%s/memberships/%s".formatted(personId, organizationId);
  }

  static String state(String memberState) {
    return "{\"memberState\":\"%s\"}".formatted(memberState);
  }

  void put(String subject, String uri, String body) {
    assertThat(status(subject, HttpMethod.PUT, uri, body)).isEqualTo(204);
  }

  String stateIn(String personId, String organizationId) {
    for (var person :
        json(alice, "/people?organizationId=" + organizationId + "&size=100").get("content")) {
      if (person.get("id").asText().equals(personId)) {
        return person.get("memberState").asText();
      }
    }
    throw new AssertionError("Person " + personId + " is not listed in " + organizationId);
  }

  java.util.List<String> namesIn(String organizationId, String memberState) {
    var names = new java.util.ArrayList<String>();
    var uri = "/people?organizationId=" + organizationId + "&size=100";
    if (memberState != null) {
      uri += "&memberState=" + memberState;
    }
    json(alice, uri).get("content").forEach(person -> names.add(person.get("fullName").asText()));
    return names;
  }

  String createGroup(String name, String type) {
    return read(body(
            alice,
            HttpMethod.POST,
            "/organizations/locations/childs",
            "{\"name\":\"%s\",\"type\":\"%s\",\"parentId\":\"%s\"}"
                .formatted(name, type, location)))
        .get("id")
        .asText();
  }

  String createPerson(String firstName, String organizationId, String memberState) {
    return read(body(
            alice,
            HttpMethod.POST,
            "/people",
            ("{\"firstName\":\"%s\",\"lastName\":\"Prueba\",\"organizationId\":\"%s\","
                    + "\"memberState\":\"%s\"}")
                .formatted(firstName, organizationId, memberState)))
        .asText();
  }

  String signUp(String name) {
    var subject = "auth0|" + name + "-" + UUID.randomUUID();
    syncCurrentUserCommandHandler.handle(
        new SyncCurrentUserCommand(subject, name + "@example.org", true));
    return subject;
  }

  int status(String subject, HttpMethod method, String uri) {
    return status(subject, method, uri, null);
  }

  int status(String subject, HttpMethod method, String uri, String body) {
    var request =
        client
            .method(method)
            .uri(uri)
            .headers(h -> h.setBearerAuth(token(subject)))
            .contentType(MediaType.APPLICATION_JSON);
    if (body != null) {
      request.body(body);
    }
    return request.exchange().returnResult(String.class).getStatus().value();
  }

  String body(String subject, HttpMethod method, String uri, String body) {
    return client
        .method(method)
        .uri(uri)
        .headers(h -> h.setBearerAuth(token(subject)))
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange()
        .expectStatus()
        .is2xxSuccessful()
        .expectBody(String.class)
        .returnResult()
        .getResponseBody();
  }

  JsonNode json(String subject, String uri) {
    return read(
        client
            .get()
            .uri(uri)
            .headers(h -> h.setBearerAuth(token(subject)))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody());
  }

  static JsonNode read(String body) {
    try {
      return JSON.readTree(body);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  String token(String subject) {
    return TestSecurityConfig.validToken(jwtEncoder, subject);
  }
}
