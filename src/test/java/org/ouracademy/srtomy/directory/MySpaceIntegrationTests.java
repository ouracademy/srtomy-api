package org.ouracademy.srtomy.directory;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.shared.SrtomyApplication;
import org.ouracademy.srtomy.shared.TestSecurityConfig;
import org.ouracademy.srtomy.user.command.link_user_to_person.LinkUserToPersonCommand;
import org.ouracademy.srtomy.user.command.link_user_to_person.LinkUserToPersonCommandHandler;
import org.ouracademy.srtomy.user.command.sync_current_user.SyncCurrentUserCommand;
import org.ouracademy.srtomy.user.command.sync_current_user.SyncCurrentUserCommandHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * "Mi espacio" end to end over the real schema: the Member level of a linked person without a
 * position, and the own-data endpoints (nodes, events) that never show anyone else's.
 */
@SpringBootTest(classes = SrtomyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class MySpaceIntegrationTests {

  static final ObjectMapper JSON = new ObjectMapper();

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  @Autowired LinkUserToPersonCommandHandler linkUserToPersonCommandHandler;

  RestTestClient client;
  String alice;
  String bob;
  String carol;
  String dan;
  String churchId;
  String location;
  String group;
  String subgroup;
  String sibling;
  String courseId;
  String bobsPerson;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port + "/v1").build();
    alice = signUp("alice", true);
    bob = signUp("bob", true);
    carol = signUp("carol", true);
    dan = signUp("dan", false);
    churchId =
        read(body(
                alice,
                HttpMethod.POST,
                "/organizations",
                """
                    {"name":"Iglesia","location":{"name":"Sede"},
                     "positions":[{"name":"Pastor","roleIds":["%s"]}],
                     "creator":{"firstName":"Ana","lastName":"Diaz","positionName":"Pastor"}}
                    """
                    .formatted(PredefinedRoles.ADMIN.value())))
            .get("id")
            .asText();
    location = json(alice, "/organizations/" + churchId + "/childs").get(0).get("id").asText();
    group = createGroup("Jóvenes", location, null);
    subgroup = createGroup("Adolescentes", group, null);
    sibling = createGroup("Alabanza", location, null);
    courseId = createCourse("Discipulado");
    bobsPerson = createPerson("Bob", group);
    link(bob, bobsPerson);
  }

  @Test
  void aLinkedPersonWithoutPositionIsAMemberOfTheirNodeAndBelow() {
    assertThat(permissions(bob, group)).containsExactlyInAnyOrder("org.read", "events.read");
    assertThat(permissions(bob, subgroup)).containsExactlyInAnyOrder("org.read", "events.read");
    // The membership was on the group, so its siblings and its location stay out of reach.
    assertThat(permissions(bob, sibling)).isEmpty();
    assertThat(permissions(bob, location)).isEmpty();
    assertThat(status(bob, HttpMethod.GET, "/organizations/" + group)).isEqualTo(200);
    assertThat(status(bob, HttpMethod.GET, "/organizations/" + sibling)).isEqualTo(403);
    // Read-only: no people, no access screen, no administration.
    assertThat(status(bob, HttpMethod.GET, "/organizations/" + group + "/access")).isEqualTo(403);
    assertThat(status(bob, HttpMethod.GET, "/people?organizationId=" + group)).isEqualTo(403);
  }

  @Test
  void anAccountWithoutALinkedPersonHasNoAccess() {
    assertThat(permissions(dan, group)).isEmpty();
    assertThat(json(dan, "/me/organizations")).isEmpty();
    assertThat(json(dan, "/me/nodes")).isEmpty();
    assertThat(json(dan, "/me/events")).isEmpty();
  }

  @Test
  void theMemberLevelIsTheFloorAndNeverCapsAPosition() {
    var leaderPosition = createPosition("Líder de jóvenes", PredefinedRoles.LEADER.value());
    var person = createPerson("Carlos", group, leaderPosition);
    link(carol, person);

    assertThat(permissions(carol, group)).contains("members.write", "events.read");
    assertThat(permissions(carol, sibling)).isEmpty();
  }

  @Test
  void theAccessListLeavesOutTheImplicitMemberLevel() {
    var emails = json(alice, "/organizations/" + group + "/access").findValuesAsText("email");

    assertThat(emails).doesNotContain(email(bob));
  }

  @Test
  void aMemberIsInTheTreeAndTheDirectoryOfTheirChurch() {
    var nodes = json(bob, "/me/organizations");
    var ids = new ArrayList<String>();
    nodes.forEach(node -> ids.add(node.get("id").asText()));

    assertThat(ids).contains(churchId, group, subgroup).doesNotContain(sibling);
    // ChurchAccess.requireMember is satisfied by the same tree, so Mi espacio and the directory
    // agree.
    var directory = json(bob, "/organizations/" + churchId + "/directory");
    assertThat(directory.findValuesAsText("name")).contains("Jóvenes", "Alabanza");
    assertThat(status(carol, HttpMethod.GET, "/organizations/" + churchId + "/directory"))
        .isEqualTo(403);
  }

  @Test
  void myNodesAreOnlyTheOnesOfMyOwnPerson() {
    var leaderId = createPerson("Lucía", location);
    var led = createGroup("Misiones", location, leaderId);
    link(carol, leaderId);

    var bobs = json(bob, "/me/nodes");
    assertThat(bobs).hasSize(1);
    assertThat(bobs.get(0).get("id").asText()).isEqualTo(group);
    assertThat(bobs.get(0).get("leader").asBoolean()).isFalse();
    assertThat(bobs.get(0).get("churchName").asText()).isEqualTo("Iglesia");

    var carols = json(carol, "/me/nodes");
    var byId = new java.util.HashMap<String, JsonNode>();
    carols.forEach(node -> byId.put(node.get("id").asText(), node));
    assertThat(byId.keySet()).containsExactlyInAnyOrder(location, led);
    assertThat(byId.get(led).get("leader").asBoolean()).isTrue();
    assertThat(byId.get(location).get("leader").asBoolean()).isFalse();
  }

  @Test
  void myEventsAreTheOnesIAttendedOrSignedUpFor() {
    var event = createEvent("Retiro", group);
    var otherPerson = createPerson("Otro", group);
    assertThat(json(bob, "/me/events")).isEmpty();

    body(
        alice,
        HttpMethod.PUT,
        "/events/" + event + "/attendance",
        "{\"personIds\":[\"%s\"]}".formatted(bobsPerson));
    var attended = json(bob, "/me/events");
    assertThat(attended).hasSize(1);
    assertThat(attended.get(0).get("title").asText()).isEqualTo("Retiro");
    assertThat(attended.get(0).get("attended").asBoolean()).isTrue();
    assertThat(attended.get(0).get("registered").asBoolean()).isFalse();
    assertThat(attended.get(0).get("organizationName").asText()).isEqualTo("Jóvenes");

    // Nobody else sees Bob's attendance, nor the person that was not marked.
    assertThat(json(carol, "/me/events")).isEmpty();
    link(carol, otherPerson);
    assertThat(json(carol, "/me/events")).isEmpty();

    // Signing up for the course through the link adds it, once, with both flags when both apply.
    openCourse();
    register(bob, email(bob));
    var both = json(bob, "/me/events");
    assertThat(both).hasSize(2);
    assertThat(both.findValuesAsText("title")).containsExactlyInAnyOrder("Retiro", "Discipulado");
  }

  @Test
  void aSignUpByEmailOnlyCountsForAVerifiedEmail() {
    openCourse();
    register(null, email(dan));
    register(null, email(carol));

    // Carol verified her email, so the sign-up made with it is hers; Dan never did.
    var carols = json(carol, "/me/events");
    assertThat(carols).hasSize(1);
    assertThat(carols.get(0).get("registered").asBoolean()).isTrue();
    assertThat(json(dan, "/me/events")).isEmpty();
  }

  @Test
  void theEndpointsNeedASession() {
    assertThat(
            client.get().uri("/me/nodes").exchange().returnResult(String.class).getStatus().value())
        .isEqualTo(401);
    assertThat(
            client
                .get()
                .uri("/me/events")
                .exchange()
                .returnResult(String.class)
                .getStatus()
                .value())
        .isEqualTo(401);
  }

  // --- helpers

  void openCourse() {
    body(
        alice,
        HttpMethod.PUT,
        "/events/" + courseId + "/sharing",
        "{\"generalAccess\":\"ANYONE_WITH_LINK\",\"linkRole\":\"REGISTRANT\"}");
  }

  void register(String subject, String email) {
    var slug = json(alice, "/events/" + courseId + "/sharing").get("slug").asText();
    var request =
        client
            .post()
            .uri("/public/events/" + slug + "/registrations")
            .contentType(MediaType.APPLICATION_JSON);
    if (subject != null) {
      request.headers(h -> h.setBearerAuth(token(subject)));
    }
    request
        .body(
            "{\"firstName\":\"Nombre\",\"lastName\":\"Apellido\",\"email\":\"%s\"}"
                .formatted(email))
        .exchange()
        .expectStatus()
        .isCreated();
  }

  String createGroup(String name, String parentId, String leaderId) {
    return read(body(
            alice,
            HttpMethod.POST,
            "/organizations/locations/childs",
            "{\"name\":\"%s\",\"type\":\"GROUP\",\"parentId\":\"%s\"%s}"
                .formatted(
                    name,
                    parentId,
                    leaderId == null ? "" : ",\"leaderIds\":[\"%s\"]".formatted(leaderId))))
        .get("id")
        .asText();
  }

  String createPerson(String firstName, String nodeId, String... positionIds) {
    return read(body(
            alice,
            HttpMethod.POST,
            "/people",
            "{\"firstName\":\"%s\",\"lastName\":\"Prueba\",\"organizationId\":\"%s\",\"positionIds\":%s}"
                .formatted(firstName, nodeId, JSON.valueToTree(List.of(positionIds)))))
        .asText();
  }

  String createPosition(String name, Object roleId) {
    return read(body(
            alice,
            HttpMethod.POST,
            "/organizations/" + churchId + "/positions",
            "{\"name\":\"%s\",\"roleIds\":[\"%s\"]}".formatted(name, roleId)))
        .get("id")
        .asText();
  }

  String createCourse(String name) {
    return createEventOf(name, group, true);
  }

  String createEvent(String name, String nodeId) {
    return createEventOf(name, nodeId, false);
  }

  String createEventOf(String name, String nodeId, boolean course) {
    return read(body(
            alice,
            HttpMethod.POST,
            "/events",
            ("{\"name\":\"%s\",\"startTime\":\"2030-01-05T10:00:00-05:00\","
                    + "\"endTime\":\"2030-01-05T12:00:00-05:00\",\"organizationId\":\"%s\","
                    + "\"isACourse\":%s}")
                .formatted(name, nodeId, course)))
        .asText();
  }

  void link(String subject, String personId) {
    var user =
        syncCurrentUserCommandHandler
            .handle(new SyncCurrentUserCommand(subject, email(subject), !subject.contains("dan")))
            .user();
    linkUserToPersonCommandHandler.handle(
        new LinkUserToPersonCommand(
            user.id(),
            new PartyId(UUID.fromString(personId)),
            new PartyId(UUID.fromString(churchId))));
  }

  List<String> permissions(String subject, String nodeId) {
    var found = new ArrayList<String>();
    var response =
        client
            .get()
            .uri("/me/permissions?organizationId=" + nodeId)
            .headers(h -> h.setBearerAuth(token(subject)))
            .exchange()
            .expectBody(String.class)
            .returnResult();
    if (response.getStatus().is2xxSuccessful()) {
      read(response.getResponseBody()).forEach(value -> found.add(value.asText()));
    }
    return found;
  }

  String signUp(String name, boolean verified) {
    var subject = "auth0|" + name + "-" + UUID.randomUUID();
    syncCurrentUserCommandHandler.handle(
        new SyncCurrentUserCommand(subject, email(subject), verified));
    return subject;
  }

  static String email(String subject) {
    return subject.replace("auth0|", "") + "@example.org";
  }

  int status(String subject, HttpMethod method, String uri) {
    return client
        .method(method)
        .uri(uri)
        .headers(h -> h.setBearerAuth(token(subject)))
        .exchange()
        .returnResult(String.class)
        .getStatus()
        .value();
  }

  String body(String subject, HttpMethod method, String uri, String body) {
    var request = client.method(method).uri(uri).headers(h -> h.setBearerAuth(token(subject)));
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).body(body);
    }
    return request
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
