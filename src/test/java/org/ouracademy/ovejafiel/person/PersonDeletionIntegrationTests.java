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
 * Removing people end to end over the real schema: what stays as history, what disappears from the
 * lists, the warning for people with an account, and the "is it this person?" matches that let a
 * removed person come back.
 */
@SpringBootTest(classes = OvejafielApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class PersonDeletionIntegrationTests {

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
  String group;

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
    group =
        read(body(
                alice,
                HttpMethod.POST,
                "/organizations/locations/childs",
                "{\"name\":\"Grupo\",\"type\":\"GROUP\",\"parentId\":\"%s\"}".formatted(location)))
            .get("id")
            .asText();
  }

  @Test
  void removingFromAGroupKeepsThePersonInTheChurchOnlyWhenTheyBelongElsewhere() {
    var lucia = createPerson("Lucía", group);

    assertThat(status(alice, HttpMethod.DELETE, delete(lucia, group, false))).isEqualTo(204);

    assertThat(namesIn(churchId)).doesNotContain("Lucía Prueba");
    // History is kept: the person and their closed membership are still there.
    assertThat(
            jdbc.queryForObject(
                "select count(*) from person where id = ?::uuid and deleted_time is null",
                Integer.class,
                lucia))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from accountability where child_id = ?::uuid"
                    + " and deleted_time is not null",
                Integer.class,
                lucia))
        .isPositive();
  }

  @Test
  void removingFromTheChurchHidesThePersonButKeepsTheirRecordAndRestoringBringsThemBack() {
    var lucia = createPerson("Lucía", group);

    assertThat(status(alice, HttpMethod.DELETE, delete(lucia, churchId, false))).isEqualTo(204);

    assertThat(namesIn(churchId)).doesNotContain("Lucía Prueba");
    assertThat(
            jdbc.queryForObject(
                "select deleted_time is not null from person where id = ?::uuid",
                Boolean.class,
                lucia))
        .isTrue();
    // Gone from the lists and from reads, but the registration form still finds them.
    assertThat(status(alice, HttpMethod.GET, "/people/" + lucia)).isEqualTo(403);
    var matches = json(alice, "/people/matches?organizationId=" + location + "&searchText=Luc");
    assertThat(matches).hasSize(1);
    assertThat(matches.get(0).get("id").asText()).isEqualTo(lucia);
    assertThat(matches.get(0).get("status").asText()).isEqualTo("REMOVED");
    assertThat(matches.get(0).get("removedTime").asText()).isNotBlank();

    assertThat(status(alice, HttpMethod.POST, restore(lucia, group))).isEqualTo(200);

    assertThat(namesIn(churchId)).contains("Lucía Prueba");
    assertThat(
            jdbc.queryForObject(
                "select deleted_time is null from person where id = ?::uuid", Boolean.class, lucia))
        .isTrue();
    var again = json(alice, "/people/matches?organizationId=" + location + "&searchText=Luc");
    assertThat(again.get(0).get("status").asText()).isEqualTo("ACTIVE");
    // They are back in the group, and the old membership stays as history next to the new one.
    assertThat(
            jdbc.queryForObject(
                "select count(*) from accountability where child_id = ?::uuid",
                Integer.class,
                lucia))
        .isGreaterThan(2);
  }

  @Test
  void restoringSomeoneWhoIsAlreadyInTheGroupIsAConflict() {
    var lucia = createPerson("Lucía", group);

    assertThat(status(alice, HttpMethod.POST, restore(lucia, group))).isEqualTo(409);
  }

  @Test
  void aPersonWithAnAccountNeedsConfirmationAndIsThenUnlinkedWithoutLosingTheUser() {
    var anaId =
        jdbc.queryForObject(
            "select person_id::text from user_person where church_id = ?::uuid",
            String.class,
            churchId);
    var users = jdbc.queryForObject("select count(*) from app_user", Integer.class);

    assertThat(status(alice, HttpMethod.DELETE, delete(anaId, churchId, false))).isEqualTo(409);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from user_person where person_id = ?::uuid", Integer.class, anaId))
        .isEqualTo(1);

    assertThat(status(alice, HttpMethod.DELETE, delete(anaId, churchId, true))).isEqualTo(204);

    assertThat(
            jdbc.queryForObject(
                "select count(*) from user_person where person_id = ?::uuid", Integer.class, anaId))
        .isZero();
    assertThat(jdbc.queryForObject("select count(*) from app_user", Integer.class))
        .isEqualTo(users);
  }

  @Test
  void whoLacksMembersDeleteCannotRemoveAnyone() {
    var lucia = createPerson("Lucía", group);

    assertThat(status(bob, HttpMethod.DELETE, delete(lucia, group, false))).isEqualTo(403);
    assertThat(namesIn(churchId)).contains("Lucía Prueba");
  }

  @Test
  void matchesFindActivePeopleByNameAndByEmailAcrossTheChurch() {
    createPerson("Marta", group);
    body(
        alice,
        HttpMethod.POST,
        "/people",
        ("{\"firstName\":\"Pedro\",\"lastName\":\"Gil\",\"email\":\"pedro@example.org\","
                + "\"organizationId\":\"%s\"}")
            .formatted(location));

    var byName = json(alice, "/people/matches?organizationId=" + location + "&searchText=mart");
    assertThat(byName).hasSize(1);
    assertThat(byName.get(0).get("status").asText()).isEqualTo("ACTIVE");

    var byEmail =
        json(alice, "/people/matches?organizationId=" + group + "&email=PEDRO@example.org");
    assertThat(byEmail).hasSize(1);
    assertThat(byEmail.get(0).get("fullName").asText()).isEqualTo("Pedro Gil");

    assertThat(json(alice, "/people/matches?organizationId=" + location + "&searchText=zz"))
        .isEmpty();
  }

  // --- helpers

  static String delete(String personId, String organizationId, boolean confirm) {
    return "/people/%s?organizationId=%s&confirmUnlinkUser=%s"
        .formatted(personId, organizationId, confirm);
  }

  static String restore(String personId, String organizationId) {
    return "/people/%s/restore?organizationId=%s".formatted(personId, organizationId);
  }

  java.util.List<String> namesIn(String organizationId) {
    var names = new java.util.ArrayList<String>();
    json(alice, "/people?organizationId=" + organizationId + "&size=100")
        .get("content")
        .forEach(person -> names.add(person.get("fullName").asText()));
    return names;
  }

  String createPerson(String firstName, String organizationId) {
    return read(body(
            alice,
            HttpMethod.POST,
            "/people",
            "{\"firstName\":\"%s\",\"lastName\":\"Prueba\",\"organizationId\":\"%s\"}"
                .formatted(firstName, organizationId)))
        .asText();
  }

  String signUp(String name) {
    var subject = "auth0|" + name + "-" + UUID.randomUUID();
    syncCurrentUserCommandHandler.handle(
        new SyncCurrentUserCommand(subject, name + "@example.org", true));
    return subject;
  }

  int status(String subject, HttpMethod method, String uri) {
    return client
        .method(method)
        .uri(uri)
        .headers(h -> h.setBearerAuth(token(subject)))
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .returnResult(String.class)
        .getStatus()
        .value();
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
