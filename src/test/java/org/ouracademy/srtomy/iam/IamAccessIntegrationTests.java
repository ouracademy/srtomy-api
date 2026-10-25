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
 * The access rules end to end over the real schema: access that comes from positions held by a
 * person linked to a user, the anti-escalation rule, the Owner exceptions and the lifecycle of
 * custom access levels. Every test uses fresh random users and emails.
 */
@SpringBootTest(classes = SrtomyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class IamAccessIntegrationTests {

  static final ObjectMapper JSON = new ObjectMapper();

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  @Autowired LinkUserToPersonCommandHandler linkUserToPersonCommandHandler;

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
  void aPositionGivesItsLevelToTheLinkedUserOnTheNodeAndBelow() {
    var church = createChurch(alice);
    var group = createGroup(alice, church.location);
    var subgroup = createGroup(alice, group);
    holdPosition(bob, church, group, PredefinedRoles.LEADER.value());

    assertThat(status(bob, HttpMethod.GET, "/organizations/" + subgroup, null)).isEqualTo(200);
    assertThat(permissions(bob, subgroup)).contains("members.write").doesNotContain("iam.manage");
    // The position was given on the group, so its location stays out of reach.
    assertThat(status(bob, HttpMethod.GET, "/organizations/" + church.location, null))
        .isEqualTo(403);
  }

  @Test
  void aPositionWithoutLinkedUserOrWithoutLevelGivesNoAccess() {
    var church = createChurch(alice);
    var group = createGroup(alice, church.location);
    var withLevel =
        createPosition(alice, church.id, "Tesorero", PredefinedRoles.COLLABORATOR.value());
    var label = createPosition(alice, church.id, "Hermano", null);

    // Bob has a person with the position, but no user link yet: only a label for the app.
    createPerson(alice, "Bob", group, withLevel);
    assertThat(status(bob, HttpMethod.GET, "/organizations/" + group, null)).isEqualTo(403);

    // Linked, but holding a position without level: still nothing.
    var other = signUp("other");
    link(other, church.id, createPerson(alice, "Otro", group, label));
    assertThat(status(other, HttpMethod.GET, "/organizations/" + group, null)).isEqualTo(403);
  }

  @Test
  void removingThePositionOrChangingItsLevelTakesEffectAtOnce() {
    var church = createChurch(alice);
    var position =
        createPosition(alice, church.id, "Tesorero", PredefinedRoles.COLLABORATOR.value());
    var personId = createPerson(alice, "Bob", church.location, position);
    link(bob, church.id, personId);
    assertThat(permissions(bob, church.location)).contains("attendance.write");

    // Lowering the level changes the permissions of whoever holds the position.
    assertThat(
            status(
                alice,
                HttpMethod.PUT,
                "/positions/" + position + "/access",
                "{\"roleIds\":[\"%s\"]}".formatted(PredefinedRoles.MEMBER.value())))
        .isEqualTo(200);
    assertThat(permissions(bob, church.location))
        .containsExactlyInAnyOrder("org.read", "events.read");

    // Taking the level away leaves the position as a label.
    assertThat(status(alice, HttpMethod.PUT, "/positions/" + position + "/access", "{}"))
        .isEqualTo(200);
    assertThat(status(bob, HttpMethod.GET, "/organizations/" + church.location, null))
        .isEqualTo(403);

    // Giving it back and then removing the position from the person drops the access too.
    status(
        alice,
        HttpMethod.PUT,
        "/positions/" + position + "/access",
        "{\"roleIds\":[\"%s\"]}".formatted(PredefinedRoles.COLLABORATOR.value()));
    assertThat(status(bob, HttpMethod.GET, "/organizations/" + church.location, null))
        .isEqualTo(200);
    updatePerson(alice, personId, "Bob", church.location, null);
    assertThat(status(bob, HttpMethod.GET, "/organizations/" + church.location, null))
        .isEqualTo(403);
  }

  @Test
  void theAccessListSaysWhereEachAccessComesFrom() {
    var church = createChurch(alice);
    var group = createGroup(alice, church.location);
    holdPosition(bob, church, group, PredefinedRoles.LEADER.value());

    var access = json(alice, "/organizations/" + group + "/access");

    // Alice has two accesses: Owner (direct) and the level of her own Pastor position.
    var owner = find(access, "Propietario", "roleName");
    assertThat(owner.get("email").asText()).isEqualTo(email(alice));
    assertThat(owner.get("source").asText()).isEqualTo("DIRECT");
    var pastor = find(access, "Pastor", "positionName");
    assertThat(pastor.get("source").asText()).isEqualTo("POSITION");
    assertThat(pastor.get("roleId").isNull()).isTrue();
    assertThat(pastor.get("permissionCount").asInt()).isEqualTo(13);
    assertThat(owner.get("inherited").asBoolean()).isTrue();
    assertThat(owner.get("organizationId").asText()).isEqualTo(church.id);
    var leader = find(access, email(bob));
    assertThat(leader.get("source").asText()).isEqualTo("POSITION");
    assertThat(leader.get("positionName").asText()).startsWith("Cargo ");
    assertThat(leader.get("inherited").asBoolean()).isFalse();
    assertThat(leader.get("bindingId").isNull()).isTrue();
    assertThat(leader.get("organizationId").asText()).isEqualTo(group);

    // A leader reads the list (iam.read) but is not allowed to add Owners (iam.manage).
    assertThat(json(bob, "/organizations/" + group + "/access")).isNotEmpty();
    assertThat(addOwner(bob, group, email(carol))).isEqualTo(403);
    // Someone with no access on the node cannot even read it.
    assertThat(status(carol, HttpMethod.GET, "/organizations/" + group + "/access", null))
        .isEqualTo(403);
  }

  @Test
  void aUserWhoseCreatorPersonHoldsAPositionKeepsOwnerSeparately() {
    var church = createChurch(alice);

    var access = json(alice, "/organizations/" + church.location + "/access");

    // Alice is the Owner (direct) and her own position (Pastor, Administrator) as well.
    assertThat(access).extracting(n -> n.get("source").asText()).contains("DIRECT", "POSITION");
    var positions = json(alice, "/organizations/" + church.id + "/positions");
    var pastor = find(positions, "Pastor", "name");
    assertThat(pastor.get("holders")).hasSize(1);
    assertThat(pastor.get("holders").get(0).get("name").asText()).isEqualTo("Ana Diaz");
    assertThat(pastor.get("holders").get(0).get("email").asText()).isEqualTo(email(alice));
    assertThat(pastor.get("effectivePermissions")).hasSize(13);
  }

  @Test
  void aPositionGivesTheUnionOfItsRolesAndItsLoosePermissions() {
    var church = createChurch(alice);
    var position =
        read(body(
                alice,
                HttpMethod.POST,
                "/organizations/" + church.id + "/positions",
                """
                    {"name":"Tesorero","roleIds":["%s"],"permissions":["events.write"]}
                    """
                    .formatted(PredefinedRoles.VIEW_PEOPLE.value())))
            .get("id")
            .asText();
    link(bob, church.id, createPerson(alice, "Bob", church.location, position));

    assertThat(permissions(bob, church.location))
        .containsExactlyInAnyOrder("org.read", "members.read", "events.write");
    var summary =
        find(json(alice, "/organizations/" + church.id + "/positions"), "Tesorero", "name");
    assertThat(values(summary.get("effectivePermissions")))
        .containsExactly("events.write", "members.read", "org.read");

    // Editing the role changes the position that uses it; the loose permission stays.
    assertThat(
            status(
                alice,
                HttpMethod.PUT,
                "/positions/" + position + "/access",
                "{\"permissions\":[\"events.write\"]}"))
        .isEqualTo(200);
    assertThat(permissions(bob, church.location)).containsExactly("events.write");
  }

  @Test
  void nobodyGivesAPositionWithMoreAccessThanTheyHold() {
    var church = createChurch(alice);
    var pastor =
        find(json(alice, "/organizations/" + church.id + "/positions"), "Pastor", "name")
            .get("id")
            .asText();
    var usher = createPosition(alice, church.id, "Ujier", PredefinedRoles.COLLABORATOR.value());
    // Bob leads the location and may manage positions and people, but is no administrator.
    var manager =
        createRole(alice, church.id, "Gestor", "org.read", "members.write", "positions.manage");
    holdPosition(bob, church, church.id, manager);
    var friend = createPerson(alice, "Amigo", church.location);

    // He cannot give the Pastor position (Administrator level), nor create one like it.
    assertThat(
            status(
                bob,
                HttpMethod.PUT,
                "/people/" + friend,
                personBody("Amigo", church.location, pastor)))
        .isEqualTo(403);
    assertThat(
            status(
                bob,
                HttpMethod.POST,
                "/organizations/" + church.id + "/positions",
                "{\"name\":\"Obispo\",\"roleIds\":[\"%s\"]}"
                    .formatted(PredefinedRoles.ADMIN.value())))
        .isEqualTo(403);
    // Nor raise the level of an existing position, or delete one that gives access.
    assertThat(
            status(
                bob,
                HttpMethod.PUT,
                "/positions/" + usher + "/access",
                "{\"roleIds\":[\"%s\"]}".formatted(PredefinedRoles.ADMIN.value())))
        .isEqualTo(403);
    assertThat(status(bob, HttpMethod.DELETE, "/positions/" + pastor, null)).isEqualTo(403);
    // Labels are free for him to create and to hand out.
    var label = createPosition(bob, church.id, "Hermano", null);
    assertThat(
            status(
                bob,
                HttpMethod.PUT,
                "/people/" + friend,
                personBody("Amigo", church.location, label)))
        .isEqualTo(200);
  }

  @Test
  void anAdministratorCannotAddOrRemoveOwners() {
    var church = createChurch(alice);
    holdPosition(bob, church, church.id, PredefinedRoles.ADMIN.value());
    var ownerBinding = bindingOf(alice, church.id, email(alice));

    assertThat(addOwner(bob, church.id, email(carol))).isEqualTo(403);
    assertThat(
            status(
                bob, HttpMethod.DELETE, "/organizations/" + church.id + "/owners/" + ownerBinding))
        .isEqualTo(403);
  }

  @Test
  void anUnknownEmailIsRejected() {
    var church = createChurch(alice);
    assertThat(addOwner(alice, church.id, "nobody-" + UUID.randomUUID() + "@x.org")).isEqualTo(400);
  }

  @Test
  void theLastOwnerCannotBeRemoved() {
    var church = createChurch(alice);
    var own = bindingOf(alice, church.id, email(alice));
    var path = "/organizations/" + church.id + "/owners/";

    assertThat(status(alice, HttpMethod.DELETE, path + own)).isEqualTo(400);

    assertThat(addOwner(alice, church.id, email(bob))).isEqualTo(201);
    assertThat(status(alice, HttpMethod.DELETE, path + own)).isEqualTo(204);
    // Bob is now the only Owner and cannot remove himself either.
    var bobs = bindingOf(bob, church.id, email(bob));
    assertThat(status(bob, HttpMethod.DELETE, path + bobs)).isEqualTo(400);
  }

  @Test
  void anOwnerOfTheOtherChurchIsNotFound() {
    var church = createChurch(alice);
    var other = createChurch(bob);
    var bobs = bindingOf(bob, other.id, email(bob));

    assertThat(status(alice, HttpMethod.DELETE, "/organizations/" + church.id + "/owners/" + bobs))
        .isEqualTo(404);
  }

  @Test
  void customLevelsAreCreatedEditedAndOnlyDeletedWhenNoPositionUsesThem() {
    var church = createChurch(alice);
    var path = "/organizations/" + church.id + "/roles";

    var treasurer = createRole(alice, church.id, "Tesorero", "org.read", "events.write");
    var position = createPosition(alice, church.id, "Tesorero", treasurer);
    link(bob, church.id, createPerson(alice, "Bob", church.location, position));
    assertThat(permissions(bob, church.location))
        .containsExactlyInAnyOrder("org.read", "events.write");

    // Validation: unknown permission, no permissions, duplicate name (any case), predefined name.
    assertThat(status(alice, HttpMethod.POST, path, role("Otro", "org.fly"))).isEqualTo(400);
    assertThat(status(alice, HttpMethod.POST, path, role("Otro"))).isEqualTo(400);
    assertThat(status(alice, HttpMethod.POST, path, role("tesorero", "org.read"))).isEqualTo(400);
    assertThat(status(alice, HttpMethod.POST, path, role("líder", "org.read"))).isEqualTo(400);

    // Editing changes the effective permissions of everyone whose position uses the level.
    assertThat(
            status(alice, HttpMethod.PUT, "/roles/" + treasurer, role("Tesorero", "events.read")))
        .isEqualTo(200);
    assertThat(permissions(bob, church.location)).containsExactly("events.read");

    // It shows next to the predefined ones.
    var roles = json(alice, path);
    assertThat(values(roles, "name")).contains("Propietario", "Líder", "Tesorero");
    assertThat(find(roles, "Tesorero", "name").get("predefined").asBoolean()).isFalse();
    assertThat(find(roles, "Propietario", "name").get("assignable").asBoolean()).isFalse();

    // Predefined levels can be neither edited nor deleted.
    var leader = "/roles/" + PredefinedRoles.LEADER.value();
    assertThat(status(alice, HttpMethod.PUT, leader, role("X", "org.read"))).isEqualTo(400);
    assertThat(status(alice, HttpMethod.DELETE, leader)).isEqualTo(400);

    // In use: cannot be deleted. After changing the position's level it can.
    assertThat(status(alice, HttpMethod.DELETE, "/roles/" + treasurer)).isEqualTo(400);
    status(alice, HttpMethod.PUT, "/positions/" + position + "/access", "{}");
    assertThat(status(alice, HttpMethod.DELETE, "/roles/" + treasurer)).isEqualTo(204);
    assertThat(values(json(alice, path), "name")).doesNotContain("Tesorero");
    // The name is free again.
    assertThat(status(alice, HttpMethod.POST, path, role("Tesorero", "org.read"))).isEqualTo(201);
  }

  @Test
  void customLevelsBelongToTheirChurch() {
    var mine = createChurch(alice);
    var theirs = createChurch(bob);
    var treasurer = createRole(alice, mine.id, "Tesorero", "org.read");

    // Bob cannot manage Alice's level, nor use it for a position of his own church.
    assertThat(status(bob, HttpMethod.PUT, "/roles/" + treasurer, role("Mío", "org.read")))
        .isEqualTo(403);
    assertThat(status(bob, HttpMethod.DELETE, "/roles/" + treasurer)).isEqualTo(403);
    assertThat(
            status(
                bob,
                HttpMethod.POST,
                "/organizations/" + theirs.id + "/positions",
                "{\"name\":\"X\",\"roleIds\":[\"%s\"]}".formatted(treasurer)))
        .isEqualTo(400);
    assertThat(values(json(bob, "/organizations/" + theirs.id + "/roles"), "name"))
        .doesNotContain("Tesorero");
  }

  @Test
  void ownerIsNeverAPositionLevel() {
    var church = createChurch(alice);

    assertThat(
            status(
                alice,
                HttpMethod.POST,
                "/organizations/" + church.id + "/positions",
                "{\"name\":\"Dueño\",\"roleIds\":[\"%s\"]}"
                    .formatted(PredefinedRoles.OWNER.value())))
        .isEqualTo(400);
    assertThat(values(json(alice, "/iam/levels"), "name"))
        .containsExactlyInAnyOrder(
            "Administrador",
            "Líder",
            "Colaborador",
            "Miembro",
            "Ver personas",
            "Gestor de personas",
            "Gestor de eventos",
            "Asistencia",
            "Gestor de estructura");
  }

  @Test
  void thePermissionCatalogIsGroupedByResource() {
    var catalog = json(alice, "/iam/permissions");

    assertThat(values(catalog, "resource"))
        .contains("org", "members", "events", "attendance", "positions", "iam", "invitations");
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

  static String role(String name, String... permissions) {
    var list = new ArrayList<String>();
    for (var permission : permissions) {
      list.add("\"" + permission + "\"");
    }
    return "{\"name\":\"%s\",\"permissions\":[%s]}".formatted(name, String.join(",", list));
  }

  String createRole(String subject, String churchId, String name, String... permissions) {
    return read(body(
            subject,
            HttpMethod.POST,
            "/organizations/" + churchId + "/roles",
            role(name, permissions)))
        .get("id")
        .asText();
  }

  /** Alice's church: a location and the positions Pastor (Administrator) and Ujier. */
  Church createChurch(String subject) {
    var created =
        body(
            subject,
            HttpMethod.POST,
            "/organizations",
            """
            {"name":"Iglesia","location":{"name":"Sede"},
             "positions":[{"name":"Pastor","roleIds":["%s"]}],
             "creator":{"firstName":"Ana","lastName":"Diaz","positionName":"Pastor"}}
            """
                .formatted(PredefinedRoles.ADMIN.value()));
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

  String createPosition(String subject, String churchId, String name, Object roleId) {
    var level = roleId == null ? "[]" : "[\"" + roleId + "\"]";
    return read(body(
            subject,
            HttpMethod.POST,
            "/organizations/" + churchId + "/positions",
            "{\"name\":\"%s\",\"roleIds\":%s}".formatted(name, level)))
        .get("id")
        .asText();
  }

  static String personBody(String firstName, String organizationId, String... positionIds) {
    var ids = new ArrayList<String>();
    for (var id : positionIds) {
      if (id != null) {
        ids.add("\"" + id + "\"");
      }
    }
    return "{\"firstName\":\"%s\",\"lastName\":\"Prueba\",\"organizationId\":\"%s\",\"positionIds\":[%s]}"
        .formatted(firstName, organizationId, String.join(",", ids));
  }

  String createPerson(
      String subject, String firstName, String organizationId, String... positionIds) {
    return read(body(
            subject,
            HttpMethod.POST,
            "/people",
            personBody(firstName, organizationId, positionIds)))
        .asText();
  }

  void updatePerson(
      String subject, String personId, String firstName, String organizationId, String positionId) {
    assertThat(
            status(
                subject,
                HttpMethod.PUT,
                "/people/" + personId,
                personBody(firstName, organizationId, positionId)))
        .isEqualTo(200);
  }

  /** Links the user to a person of the church, as accepting an invitation will do. */
  void link(String subject, String churchId, String personId) {
    var user =
        syncCurrentUserCommandHandler
            .handle(new SyncCurrentUserCommand(subject, email(subject), true))
            .user();
    linkUserToPersonCommandHandler.handle(
        new LinkUserToPersonCommand(
            user.id(),
            new PartyId(UUID.fromString(personId)),
            new PartyId(UUID.fromString(churchId))));
  }

  /** Gives {@code subject} a fresh position with {@code roleId} on {@code nodeId}. */
  void holdPosition(String subject, Church church, String nodeId, Object roleId) {
    var position = createPosition(alice, church.id, "Cargo " + UUID.randomUUID(), roleId);
    link(subject, church.id, createPerson(alice, "Persona", nodeId, position));
  }

  int addOwner(String subject, String organizationId, String email) {
    return status(
        subject,
        HttpMethod.POST,
        "/organizations/" + organizationId + "/owners",
        "{\"email\":\"%s\"}".formatted(email));
  }

  String bindingOf(String subject, String organizationId, String email) {
    return find(json(subject, "/organizations/" + organizationId + "/access"), email)
        .get("bindingId")
        .asText();
  }

  List<String> permissions(String subject, String organizationId) {
    return values(json(subject, "/me/permissions?organizationId=" + organizationId));
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
