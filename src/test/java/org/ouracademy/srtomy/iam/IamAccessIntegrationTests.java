package org.ouracademy.srtomy.iam;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * The "Accesos" rules end to end over the real schema: inherited listing, anti-escalation, the
 * Owner exceptions and the custom role lifecycle. Every test uses fresh random users and emails.
 */
@SpringBootTest(classes = SrtomyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class IamAccessIntegrationTests {

  static final ObjectMapper JSON = new ObjectMapper();

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;

  RestTestClient client;
  String alice;
  String bob;
  String carol;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port + "/v1").build();
    alice = signUp("alice");
    bob = signUp("bob");
    carol = signUp("carol");
  }

  @Test
  void theAccessListMarksInheritedAndDirectBindings() {
    var church = createChurch(alice);
    var group = createGroup(alice, church.location);
    assertThat(grant(alice, group, email(bob), PredefinedRoles.LEADER.value())).isEqualTo(201);

    var access = json(alice, "/organizations/" + group + "/access");

    assertThat(access).hasSize(2);
    var owner = find(access, email(alice));
    assertThat(owner.get("roleName").asText()).isEqualTo("Propietario");
    assertThat(owner.get("inherited").asBoolean()).isTrue();
    assertThat(owner.get("organizationId").asText()).isEqualTo(church.id);
    var leader = find(access, email(bob));
    assertThat(leader.get("inherited").asBoolean()).isFalse();
    assertThat(leader.get("organizationId").asText()).isEqualTo(group);

    // A leader reads the list (iam.read) but cannot change it (iam.manage).
    assertThat(json(bob, "/organizations/" + group + "/access")).hasSize(2);
    assertThat(grant(bob, group, email(carol), PredefinedRoles.MEMBER.value())).isEqualTo(403);
    // Granting twice is idempotent.
    assertThat(grant(alice, group, email(bob), PredefinedRoles.LEADER.value())).isEqualTo(201);
    assertThat(json(alice, "/organizations/" + group + "/access")).hasSize(2);
    // Someone with no role on the node cannot even read it.
    assertThat(status(carol, HttpMethod.GET, "/organizations/" + group + "/access", null))
        .isEqualTo(403);
  }

  @Test
  void anUnknownEmailIsRejected() {
    var church = createChurch(alice);
    assertThat(grant(alice, church.id, "nobody-" + UUID.randomUUID() + "@x.org", role(0)))
        .isEqualTo(400);
  }

  @Test
  void anAdministratorCannotGrantOrRevokeOwner() {
    var church = createChurch(alice);
    assertThat(grant(alice, church.id, email(bob), PredefinedRoles.ADMIN.value())).isEqualTo(201);

    assertThat(grant(bob, church.id, email(carol), PredefinedRoles.OWNER.value())).isEqualTo(403);
    var ownerBinding = bindingOf(alice, church.id, email(alice));
    assertThat(
            status(
                bob, HttpMethod.DELETE, "/organizations/" + church.id + "/access/" + ownerBinding))
        .isEqualTo(403);
    // But an Administrator hands out other roles freely.
    assertThat(grant(bob, church.id, email(carol), PredefinedRoles.LEADER.value())).isEqualTo(201);
  }

  @Test
  void theLastOwnerCannotBeRemoved() {
    var church = createChurch(alice);
    var own = bindingOf(alice, church.id, email(alice));
    var path = "/organizations/" + church.id + "/access/";

    assertThat(status(alice, HttpMethod.DELETE, path + own)).isEqualTo(400);

    assertThat(grant(alice, church.id, email(bob), PredefinedRoles.OWNER.value())).isEqualTo(201);
    assertThat(status(alice, HttpMethod.DELETE, path + own)).isEqualTo(204);
    // Bob is now the only Owner and cannot remove himself either.
    var bobs = bindingOf(bob, church.id, email(bob));
    assertThat(status(bob, HttpMethod.DELETE, path + bobs)).isEqualTo(400);
  }

  @Test
  void anInheritedBindingCannotBeRevokedFromADescendant() {
    var church = createChurch(alice);
    var group = createGroup(alice, church.location);
    var inherited = bindingOf(alice, group, email(alice));

    assertThat(status(alice, HttpMethod.DELETE, "/organizations/" + group + "/access/" + inherited))
        .isEqualTo(404);
  }

  @Test
  void nobodyGrantsMoreThanTheyHold() {
    var church = createChurch(alice);
    var manager =
        roleId(
            body(
                alice,
                HttpMethod.POST,
                "/organizations/" + church.id + "/roles",
                role("Gestor", "org.read", "iam.read", "iam.manage")));
    assertThat(grant(alice, church.id, email(bob), manager)).isEqualTo(201);

    // Member needs events.read, which the manager lacks; Leader needs far more.
    assertThat(grant(bob, church.id, email(carol), PredefinedRoles.MEMBER.value())).isEqualTo(403);
    assertThat(grant(bob, church.id, email(carol), PredefinedRoles.LEADER.value())).isEqualTo(403);
    // The role they hold themselves is fine.
    assertThat(grant(bob, church.id, email(carol), manager)).isEqualTo(201);
    // And they cannot mint a role with permissions they lack.
    assertThat(
            status(
                bob,
                HttpMethod.POST,
                "/organizations/" + church.id + "/roles",
                role("Truco", "org.read", "events.write")))
        .isEqualTo(403);
  }

  @Test
  void customRolesAreCreatedEditedAndSoftDeleted() {
    var church = createChurch(alice);
    var path = "/organizations/" + church.id + "/roles";

    var created = body(alice, HttpMethod.POST, path, role("Tesorero", "org.read", "events.write"));
    var treasurer = roleId(created);
    assertThat(grant(alice, church.location, email(bob), treasurer)).isEqualTo(201);
    assertThat(permissions(bob, church.location))
        .containsExactlyInAnyOrder("org.read", "events.write");

    // Validation: unknown permission, no permissions, duplicate name (any case), predefined name.
    assertThat(status(alice, HttpMethod.POST, path, role("Otro", "org.fly"))).isEqualTo(400);
    assertThat(status(alice, HttpMethod.POST, path, role("Otro"))).isEqualTo(400);
    assertThat(status(alice, HttpMethod.POST, path, role("tesorero", "org.read"))).isEqualTo(400);
    assertThat(status(alice, HttpMethod.POST, path, role("líder", "org.read"))).isEqualTo(400);

    // Editing changes the effective permissions of everyone who has the role.
    assertThat(
            status(alice, HttpMethod.PUT, "/roles/" + treasurer, role("Tesorero", "events.read")))
        .isEqualTo(200);
    assertThat(permissions(bob, church.location)).containsExactly("events.read");

    // It shows in the church's role list next to the predefined ones.
    var roles = json(alice, path);
    assertThat(values(roles, "name")).contains("Propietario", "Líder", "Tesorero");
    assertThat(find(roles, "Tesorero", "name").get("predefined").asBoolean()).isFalse();

    // Predefined roles can be neither edited nor deleted.
    var leader = "/roles/" + PredefinedRoles.LEADER.value();
    assertThat(status(alice, HttpMethod.PUT, leader, role("X", "org.read"))).isEqualTo(400);
    assertThat(status(alice, HttpMethod.DELETE, leader)).isEqualTo(400);

    // Soft delete: gone from the list and from the access list, and bob loses its permissions.
    assertThat(status(alice, HttpMethod.DELETE, "/roles/" + treasurer)).isEqualTo(204);
    assertThat(values(json(alice, path), "name")).doesNotContain("Tesorero");
    assertThat(json(alice, "/organizations/" + church.location + "/access"))
        .noneMatch(entry -> entry.get("roleName").asText().equals("Tesorero"));
    assertThat(permissionsStatus(bob, church.location)).isEqualTo(403);
    assertThat(grant(alice, church.location, email(carol), treasurer)).isEqualTo(404);
    // The name is free again.
    assertThat(status(alice, HttpMethod.POST, path, role("Tesorero", "org.read"))).isEqualTo(201);
  }

  @Test
  void customRolesBelongToTheirChurch() {
    var mine = createChurch(alice);
    var theirs = createChurch(bob);
    var treasurer =
        roleId(
            body(
                alice,
                HttpMethod.POST,
                "/organizations/" + mine.id + "/roles",
                role("Tesorero", "org.read")));

    // Bob cannot manage Alice's role, nor grant it in his own church.
    assertThat(status(bob, HttpMethod.PUT, "/roles/" + treasurer, role("Mío", "org.read")))
        .isEqualTo(403);
    assertThat(status(bob, HttpMethod.DELETE, "/roles/" + treasurer)).isEqualTo(403);
    assertThat(grant(bob, theirs.id, email(carol), treasurer)).isEqualTo(400);
    // Nor does it show in his church's role list.
    assertThat(values(json(bob, "/organizations/" + theirs.id + "/roles"), "name"))
        .doesNotContain("Tesorero");
  }

  @Test
  void thePermissionCatalogIsGroupedByResource() {
    var catalog = json(alice, "/iam/permissions");

    assertThat(values(catalog, "resource"))
        .contains("org", "members", "events", "attendance", "positions", "iam");
    assertThat(find(catalog, "iam", "resource").get("permissions").toString())
        .contains("iam.read", "iam.manage");
  }

  // --- helpers

  record Church(String id, String location) {}

  String signUp(String name) {
    var subject = "auth0|" + name + "-" + UUID.randomUUID();
    syncCurrentUserCommandHandler.handle(new SyncCurrentUserCommand(subject, email(subject), true));
    return subject;
  }

  /** Emails are derived from the subject so each test user has its own. */
  static String email(String subject) {
    return subject.replace("auth0|", "") + "@example.org";
  }

  static String role(int index) {
    return PredefinedRoles.MEMBER.value().toString();
  }

  static String role(String name, String... permissions) {
    var list = new ArrayList<String>();
    for (var permission : permissions) {
      list.add("\"" + permission + "\"");
    }
    return "{\"name\":\"%s\",\"permissions\":[%s]}".formatted(name, String.join(",", list));
  }

  Church createChurch(String subject) {
    var created =
        body(
            subject,
            HttpMethod.POST,
            "/organizations",
            "{\"name\":\"Iglesia\",\"location\":{\"name\":\"Sede\"},\"positions\":[]}");
    var churchId = read(created).get("id").asText();
    var location = json(subject, "/organizations/" + churchId + "/childs").get(0);
    return new Church(churchId, location.get("id").asText());
  }

  String createGroup(String subject, String parentId) {
    var created =
        body(
            subject,
            HttpMethod.POST,
            "/organizations/locations/childs",
            "{\"name\":\"Grupo\",\"type\":\"GROUP\",\"parentId\":\"%s\"}".formatted(parentId));
    return read(created).get("id").asText();
  }

  int grant(String subject, String organizationId, String email, Object roleId) {
    return status(
        subject,
        HttpMethod.POST,
        "/organizations/" + organizationId + "/access",
        "{\"email\":\"%s\",\"roleId\":\"%s\"}".formatted(email, roleId));
  }

  String bindingOf(String subject, String organizationId, String email) {
    return find(json(subject, "/organizations/" + organizationId + "/access"), email)
        .get("bindingId")
        .asText();
  }

  static String roleId(String createdRoleJson) {
    return read(createdRoleJson).get("id").asText();
  }

  List<String> permissions(String subject, String organizationId) {
    return values(json(subject, "/me/permissions?organizationId=" + organizationId));
  }

  int permissionsStatus(String subject, String organizationId) {
    return status(subject, HttpMethod.GET, "/organizations/" + organizationId, null);
  }

  static JsonNode find(JsonNode array, String email) {
    return find(array, email, "email");
  }

  static JsonNode find(JsonNode array, String value, String field) {
    for (var node : array) {
      if (value.equals(node.get(field).asText())) {
        return node;
      }
    }
    throw new AssertionError("No element with " + field + " = " + value + " in " + array);
  }

  int status(String subject, HttpMethod method, String uri) {
    return status(subject, method, uri, null);
  }

  int status(String subject, HttpMethod method, String uri, String body) {
    var request = client.method(method).uri(uri).headers(h -> h.setBearerAuth(token(subject)));
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).body(body);
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

  static List<String> values(JsonNode array, String field) {
    var values = new ArrayList<String>();
    array.forEach(node -> values.add(node.get(field).asText()));
    return values;
  }

  static List<String> values(JsonNode array) {
    var values = new ArrayList<String>();
    array.forEach(node -> values.add(node.asText()));
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
