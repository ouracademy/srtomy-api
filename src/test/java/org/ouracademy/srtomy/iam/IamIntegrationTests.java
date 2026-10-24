package org.ouracademy.srtomy.iam;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.iam.command.grant_role.GrantRoleCommand;
import org.ouracademy.srtomy.iam.command.grant_role.GrantRoleCommandHandler;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.shared.SrtomyApplication;
import org.ouracademy.srtomy.shared.TestSecurityConfig;
import org.ouracademy.srtomy.user.command.sync_current_user.SyncCurrentUserCommand;
import org.ouracademy.srtomy.user.command.sync_current_user.SyncCurrentUserCommandHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * End to end over the real schema: the recursive CTEs, the predefined roles seeded by the migration
 * and the enforcement in the controllers. Every test uses fresh random subjects, so the data other
 * tests leave in the shared database never matches.
 */
@SpringBootTest(classes = SrtomyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class IamIntegrationTests {

  static final ObjectMapper JSON = new ObjectMapper();

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired GrantRoleCommandHandler grantRoleCommandHandler;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;

  RestTestClient client;
  String alice;
  String bob;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port + "/v1").build();
    alice = "auth0|alice-" + UUID.randomUUID();
    bob = "auth0|bob-" + UUID.randomUUID();
  }

  @Test
  void theCreatorOwnsTheChurchAndEveryNodeBelowItButOthersSeeNothing() {
    var church = createChurch(alice);
    var location = church.location;
    var ministry = createChild(alice, location, "MINISTRY", "Alabanza");
    var choir = createChild(alice, ministry, "GROUP", "Coro");
    var voices = createChild(alice, choir, "GROUP", "Voces");
    var sopranos = createChild(alice, voices, "GROUP", "Sopranos");

    // Five levels below the church, reached through the Owner binding on the church.
    assertThat(status(alice, HttpMethod.GET, "/organizations/" + sopranos, null).value())
        .isEqualTo(200);
    assertThat(status(bob, HttpMethod.GET, "/organizations/" + church.id, null).value())
        .isEqualTo(403);
    assertThat(status(bob, HttpMethod.GET, "/organizations/" + sopranos, null).value())
        .isEqualTo(403);

    var aliceTree = json(alice, "/me/organizations");
    assertThat(values(aliceTree, "name"))
        .contains("Iglesia de prueba", "Sede Centro", "Alabanza", "Coro", "Voces", "Sopranos");
    assertThat(aliceTree.get(0).get("accessible").asBoolean()).isTrue();
    assertThat(json(bob, "/me/organizations")).isEmpty();
  }

  @Test
  void aLeaderOfAGroupReachesItsSubgroupsButNotItsLocation() {
    var church = createChurch(alice);
    var ministry = createChild(alice, church.location, "MINISTRY", "Jóvenes");
    var group = createChild(alice, ministry, "GROUP", "Célula");
    var subgroup = createChild(alice, group, "GROUP", "Subcélula");
    grant(bob, PredefinedRoles.LEADER, group);

    assertThat(status(bob, HttpMethod.GET, "/organizations/" + subgroup, null).value())
        .isEqualTo(200);
    assertThat(
            status(
                    bob,
                    HttpMethod.PUT,
                    "/organizations/locations/childs/" + subgroup,
                    "{\"name\":\"Renombrado\",\"type\":\"GROUP\"}")
                .value())
        .isEqualTo(200);
    assertThat(status(bob, HttpMethod.GET, "/organizations/" + church.location, null).value())
        .isEqualTo(403);
    assertThat(
            status(bob, HttpMethod.PUT, "/organizations/" + church.location, "{\"name\":\"X\"}")
                .value())
        .isEqualTo(403);
    assertThat(
            status(bob, HttpMethod.GET, "/people?organizationId=" + church.location, null).value())
        .isEqualTo(403);

    var permissions = json(bob, "/me/permissions?organizationId=" + subgroup);
    assertThat(permissions.toString()).contains("members.write").doesNotContain("positions.manage");

    // Bob sees the breadcrumb up to the church, but only his group and below are accessible.
    var tree = json(bob, "/me/organizations");
    for (var node : tree) {
      var accessible = node.get("accessible").asBoolean();
      var id = node.get("id").asText();
      if (id.equals(church.id) || id.equals(church.location) || id.equals(ministry)) {
        assertThat(accessible).as(node.get("name").asText()).isFalse();
      } else {
        assertThat(accessible).as(node.get("name").asText()).isTrue();
      }
    }
    assertThat(values(tree, "id")).contains(church.id, group, subgroup);
  }

  @Test
  void theTreeShapeIsEnforced() {
    var church = createChurch(alice);

    assertThat(
            status(
                    alice,
                    HttpMethod.POST,
                    "/organizations/locations/childs",
                    "{\"name\":\"Mal\",\"type\":\"GROUP\",\"parentId\":\"" + church.id + "\"}")
                .value())
        .isEqualTo(400);
  }

  @Test
  void positionsAreCreatedWithTheChurchAndManagedOnlyWithPositionsManage() {
    var church = createChurch(alice);
    grant(bob, PredefinedRoles.MEMBER, church.id);

    var positions = json(alice, "/organizations/" + church.location + "/positions");
    assertThat(values(positions, "name")).containsExactly("Pastor", "Ujier");

    assertThat(
            status(
                    alice,
                    HttpMethod.POST,
                    "/organizations/" + church.id + "/positions",
                    "{\"name\":\"pastor\"}")
                .value())
        .isEqualTo(400);
    assertThat(
            status(
                    bob,
                    HttpMethod.POST,
                    "/organizations/" + church.id + "/positions",
                    "{\"name\":\"Diácono\"}")
                .value())
        .isEqualTo(403);
    assertThat(json(bob, "/organizations/" + church.id + "/positions")).hasSize(2);

    var usher = positions.get(1).get("id").asText();
    assertThat(
            status(alice, HttpMethod.PUT, "/positions/" + usher, "{\"name\":\"Servidor\"}").value())
        .isEqualTo(200);
    assertThat(values(json(alice, "/organizations/" + church.id + "/positions"), "name"))
        .containsExactly("Pastor", "Servidor");
  }

  @Test
  void aPersonGetsOnlyPositionsOfTheirOwnChurch() throws Exception {
    var mine = createChurch(alice);
    var other = createChurch(bob);
    var myPastor =
        json(alice, "/organizations/" + mine.id + "/positions").get(0).get("id").asText();
    var foreignPastor =
        json(bob, "/organizations/" + other.id + "/positions").get(0).get("id").asText();

    var personId =
        JSON.readTree(
                body(
                    alice,
                    HttpMethod.POST,
                    "/people",
                    """
                    {"firstName":"Ana","lastName":"Diaz","organizationId":"%s",
                     "positionIds":["%s","%s"]}
                    """
                        .formatted(mine.location, myPastor, foreignPastor)))
            .asText();

    var held =
        json(alice, "/organizations/" + mine.location + "/people/" + personId + "/positions");
    assertThat(values(held, "id")).containsExactly(myPastor);
    assertThat(status(bob, HttpMethod.GET, "/people/" + personId, null).value()).isEqualTo(403);
  }

  record Church(String id, String location) {}

  Church createChurch(String subject) {
    var created =
        body(
            subject,
            HttpMethod.POST,
            "/organizations",
            """
            {"name":"Iglesia de prueba",
             "socialLinks":{"youtubeUrl":"https://youtube.com/@iglesia"},
             "location":{"name":"Sede Centro"},
             "positions":["Pastor","Ujier","PASTOR"]}
            """);
    var churchId = read(created).get("id").asText();
    var location = json(subject, "/organizations/" + churchId + "/childs").get(0);
    assertThat(location.get("parentId").asText()).isEqualTo(churchId);
    return new Church(churchId, location.get("id").asText());
  }

  String createChild(String subject, String parentId, String type, String name) {
    var created =
        body(
            subject,
            HttpMethod.POST,
            "/organizations/locations/childs",
            "{\"name\":\"%s\",\"type\":\"%s\",\"parentId\":\"%s\"}"
                .formatted(name, type, parentId));
    return read(created).get("id").asText();
  }

  void grant(String subject, IamRoleId role, String organizationId) {
    var user =
        syncCurrentUserCommandHandler
            .handle(new SyncCurrentUserCommand(subject, null, false))
            .user();
    grantRoleCommandHandler.handle(
        new GrantRoleCommand(user.id(), role, new PartyId(UUID.fromString(organizationId)), null));
  }

  HttpStatusCode status(String subject, HttpMethod method, String uri, String body) {
    var request = client.method(method).uri(uri).headers(h -> h.setBearerAuth(token(subject)));
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).body(body);
    }
    return request.exchange().returnResult(String.class).getStatus();
  }

  String body(String subject, HttpMethod method, String uri, String body) {
    var result =
        client
            .method(method)
            .uri(uri)
            .headers(h -> h.setBearerAuth(token(subject)))
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(String.class)
            .returnResult();
    return result.getResponseBody();
  }

  JsonNode json(String subject, String uri) {
    var result =
        client
            .get()
            .uri(uri)
            .headers(h -> h.setBearerAuth(token(subject)))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult();
    return read(result.getResponseBody());
  }

  /** The {@code field} of every element of a JSON array, as text. */
  static List<String> values(JsonNode array, String field) {
    var values = new ArrayList<String>();
    array.forEach(node -> values.add(node.get(field).asText()));
    return values;
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
