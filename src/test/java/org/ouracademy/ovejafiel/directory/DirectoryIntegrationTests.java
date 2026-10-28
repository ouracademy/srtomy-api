package org.ouracademy.ovejafiel.directory;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.ouracademy.ovejafiel.user.command.link_user_to_person.LinkUserToPersonCommand;
import org.ouracademy.ovejafiel.user.command.link_user_to_person.LinkUserToPersonCommandHandler;
import org.ouracademy.ovejafiel.user.command.sync_current_user.SyncCurrentUserCommand;
import org.ouracademy.ovejafiel.user.command.sync_current_user.SyncCurrentUserCommandHandler;
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
 * Visibility end to end over the real schema: the directory of a church, who sees the people of a
 * node, the public page and the minors that are never listed.
 */
@SpringBootTest(classes = OvejafielApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class DirectoryIntegrationTests {

  static final ObjectMapper JSON = new ObjectMapper();

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  @Autowired LinkUserToPersonCommandHandler linkUserToPersonCommandHandler;

  RestTestClient client;
  String alice;
  String bob;
  String carol;
  String churchId;
  String location;
  String adultLeader;
  String adultMember;
  String minorMember;
  String unknownAgeMember;
  String group;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port + "/v1").build();
    alice = signUp("alice");
    bob = signUp("bob");
    carol = signUp("carol");
    var created =
        read(
            body(
                alice,
                HttpMethod.POST,
                "/organizations",
                """
                {"name":"Iglesia","location":{"name":"Sede","address":"Av. 1"},
                 "positions":[{"name":"Pastor","roleIds":["%s"]}],
                 "creator":{"firstName":"Ana","lastName":"Diaz","positionName":"Pastor"}}
                """
                    .formatted(PredefinedRoles.ADMIN.value())));
    churchId = created.get("id").asText();
    location = json(alice, "/organizations/" + churchId + "/childs").get(0).get("id").asText();
    adultLeader = person("Lía", "Líder", LocalDate.now().minusYears(35));
    adultMember = person("Marta", "Adulta", LocalDate.now().minusYears(40));
    minorMember = person("Pablo", "Menor", LocalDate.now().minusYears(12));
    unknownAgeMember = person("Sara", "SinEdad", null);
    group =
        read(body(
                alice,
                HttpMethod.POST,
                "/organizations/locations/childs",
                ("{\"name\":\"Jóvenes\",\"type\":\"GROUP\",\"parentId\":\"%s\","
                        + "\"schedule\":\"Sábados 5 pm\",\"leaderIds\":[\"%s\"],"
                        + "\"memberIds\":[\"%s\",\"%s\",\"%s\"]}")
                    .formatted(location, adultLeader, adultMember, minorMember, unknownAgeMember)))
            .get("id")
            .asText();
  }

  @Test
  void aNewNodeIsPublicWithAnUnguessableAddressAndItsOrgChart() {
    var node = json(alice, "/organizations/" + group);

    assertThat(node.get("memberVisibility").asText()).isEqualTo("PUBLIC");
    assertThat(node.get("showOrgChart").asBoolean()).isTrue();
    assertThat(node.get("schedule").asText()).isEqualTo("Sábados 5 pm");
    assertThat(node.get("slug").asText()).hasSize(16);
    assertThat(json(alice, "/organizations/" + location).get("slug").asText())
        .isNotEqualTo(node.get("slug").asText());
  }

  @Test
  void theDirectoryShowsEveryNodeWithItsLeadersToAnyoneInTheChurch() {
    setVisibility(group, "LEADERS");
    // Bob only has a label-level access in another group of the church.
    var other = createGroup("Otro");
    holdAccess(bob, other, PredefinedRoles.MEMBER.value());

    var directory = json(bob, "/organizations/" + churchId + "/directory");

    var names = new ArrayList<String>();
    directory.forEach(node -> names.add(node.get("name").asText()));
    assertThat(names).contains("Iglesia", "Sede", "Jóvenes", "Otro");
    var youth = find(directory, "Jóvenes");
    assertThat(youth.get("schedule").asText()).isEqualTo("Sábados 5 pm");
    assertThat(youth.get("leaders").get(0).asText()).isEqualTo("Lía Líder");
    assertThat(youth.get("memberVisibility").asText()).isEqualTo("LEADERS");
  }

  @Test
  void aStrangerCannotSeeTheDirectoryNorEntries() {
    assertThat(status(carol, HttpMethod.GET, "/organizations/" + churchId + "/directory"))
        .isEqualTo(403);
    assertThat(status(carol, HttpMethod.GET, "/organizations/" + group + "/directory-entry"))
        .isEqualTo(403);
  }

  @Test
  void whoSeesThePeopleFollowsTheVisibilityOfTheNode() {
    var other = createGroup("Otro");
    holdAccess(bob, other, PredefinedRoles.MEMBER.value());

    // PUBLIC and CHURCH: anyone in the church.
    for (var visibility : List.of("PUBLIC", "CHURCH")) {
      setVisibility(group, visibility);
      assertThat(entry(bob).get("membersVisible").asBoolean()).isTrue();
    }

    // MEMBERS: not for an outsider of the group, yes for who can read its people.
    setVisibility(group, "MEMBERS");
    assertThat(entry(bob).get("membersVisible").asBoolean()).isFalse();
    assertThat(entry(bob).get("members")).isEmpty();
    assertThat(entry(alice).get("membersVisible").asBoolean()).isTrue();

    // LEADERS: only who can read its people.
    setVisibility(group, "LEADERS");
    assertThat(entry(bob).get("membersVisible").asBoolean()).isFalse();
    assertThat(entry(alice).get("membersVisible").asBoolean()).isTrue();
    // The basic information stays.
    assertThat(entry(bob).get("node").get("name").asText()).isEqualTo("Jóvenes");
  }

  @Test
  void aMemberOfTheNodeSeesItsPeopleWhenVisibilityIsMembers() {
    setVisibility(group, "MEMBERS");
    // Bob is a person of the group with a position that reads nothing but the basics.
    holdAccess(bob, group, PredefinedRoles.MEMBER.value());

    assertThat(permissions(bob, group)).isNotEmpty().doesNotContain("members.read");
    assertThat(entry(bob).get("membersVisible").asBoolean()).isTrue();
  }

  @Test
  void insideTheChurchOnlyKnownMinorsAreLeftOut() {
    var members = values(entry(alice).get("members"));

    assertThat(members).contains("Marta Adulta", "Sara SinEdad").doesNotContain("Pablo Menor");
  }

  @Test
  void thePublicPageNamesOnlyAdultsAndNeedsNoSession() {
    var slug = json(alice, "/organizations/" + group).get("slug").asText();

    var page = publicPage(slug);

    assertThat(page.get("name").asText()).isEqualTo("Jóvenes");
    assertThat(values(page.get("leaders"))).containsExactly("Lía Líder");
    assertThat(values(page.get("members")))
        .containsExactly("Marta Adulta")
        .doesNotContain("Pablo Menor", "Sara SinEdad");
    assertThat(page.get("schedule").asText()).isEqualTo("Sábados 5 pm");
    assertThat(page.has("address") && !page.get("address").isNull()).isFalse();
  }

  @Test
  void aNodeThatIsNotPublicHasNoPublicPage() {
    var slug = json(alice, "/organizations/" + group).get("slug").asText();
    for (var visibility : List.of("CHURCH", "MEMBERS", "LEADERS")) {
      setVisibility(group, visibility);
      client.get().uri("/public/organizations/" + slug).exchange().expectStatus().isNotFound();
    }
    setVisibility(group, "PUBLIC");
    client.get().uri("/public/organizations/" + slug).exchange().expectStatus().isOk();
    client.get().uri("/public/organizations/not-a-slug").exchange().expectStatus().isNotFound();
  }

  @Test
  void aChurchPageShowsTheOrgChartWithoutTheNodesOnlyLeadersSee() {
    var hidden = createGroup("Solo líderes");
    setVisibility(hidden, "LEADERS");
    var churchSlug = json(alice, "/organizations/" + churchId).get("slug").asText();

    var page = publicPage(churchSlug);

    assertThat(page.get("name").asText()).isEqualTo("Iglesia");
    var chart = page.get("orgChart");
    var locationNode = chart.get("children").get(0);
    assertThat(locationNode.get("name").asText()).isEqualTo("Sede");
    var groups = new ArrayList<String>();
    locationNode.get("children").forEach(node -> groups.add(node.get("name").asText()));
    assertThat(groups).contains("Jóvenes").doesNotContain("Solo líderes");
    assertThat(locationNode.get("children").get(0).get("slug").asText()).hasSize(16);
    assertThat(page.get("members").isNull()).isTrue();
  }

  @Test
  void theOrgChartCanBeTurnedOff() {
    body(
        alice,
        HttpMethod.PUT,
        "/organizations/" + churchId,
        "{\"name\":\"Iglesia\",\"showOrgChart\":false}");
    var churchSlug = json(alice, "/organizations/" + churchId).get("slug").asText();

    assertThat(publicPage(churchSlug).get("orgChart").isNull()).isTrue();
  }

  @Test
  void thePastorOfAPublicChurchIsListedOnlyOnceTheirAgeIsKnown() {
    var churchSlug = json(alice, "/organizations/" + churchId).get("slug").asText();
    // The creator's person has no age yet, so a public page does not name them.
    assertThat(publicPage(churchSlug).get("positions")).isEmpty();

    var pastorPerson =
        person("Pedro", "Pastor", LocalDate.now().minusYears(50), churchId, "Pastor");

    var positions = publicPage(churchSlug).get("positions");
    assertThat(positions.get(0).get("position").asText()).isEqualTo("Pastor");
    assertThat(values(positions.get(0).get("people"))).contains("Pedro Pastor");
    assertThat(pastorPerson).isNotBlank();
  }

  @Test
  void onlyWhoCanUpdateTheNodeChangesItsVisibility() {
    holdAccess(bob, group, PredefinedRoles.MEMBER.value());

    assertThat(
            status(
                bob,
                HttpMethod.PUT,
                "/organizations/locations/childs/" + group,
                updateBody("LEADERS")))
        .isEqualTo(403);
    assertThat(json(alice, "/organizations/" + group).get("memberVisibility").asText())
        .isEqualTo("PUBLIC");
  }

  // --- helpers

  String updateBody(String visibility) {
    return ("{\"name\":\"Jóvenes\",\"type\":\"GROUP\",\"memberVisibility\":\"%s\","
            + "\"leaderIds\":[\"%s\"],\"memberIds\":[\"%s\"]}")
        .formatted(visibility, adultLeader, adultMember);
  }

  void setVisibility(String nodeId, String visibility) {
    body(
        alice,
        HttpMethod.PUT,
        "/organizations/locations/childs/" + nodeId,
        ("{\"name\":\"%s\",\"type\":\"GROUP\",\"memberVisibility\":\"%s\","
                + "\"schedule\":\"Sábados 5 pm\",\"leaderIds\":[\"%s\"],\"memberIds\":[\"%s\",\"%s\",\"%s\"]}")
            .formatted(
                json(alice, "/organizations/" + nodeId).get("name").asText(),
                visibility,
                adultLeader,
                adultMember,
                minorMember,
                unknownAgeMember));
  }

  List<String> permissions(String subject, String nodeId) {
    return values(json(subject, "/me/permissions?organizationId=" + nodeId));
  }

  JsonNode entry(String subject) {
    return json(subject, "/organizations/" + group + "/directory-entry");
  }

  JsonNode publicPage(String slug) {
    return read(
        client
            .get()
            .uri("/public/organizations/" + slug)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody());
  }

  String createGroup(String name) {
    return read(body(
            alice,
            HttpMethod.POST,
            "/organizations/locations/childs",
            "{\"name\":\"%s\",\"type\":\"GROUP\",\"parentId\":\"%s\"}".formatted(name, location)))
        .get("id")
        .asText();
  }

  String person(String first, String last, LocalDate birthdate) {
    return person(first, last, birthdate, location, null);
  }

  String person(String first, String last, LocalDate birthdate, String nodeId, String position) {
    var positionIds = "[]";
    if (position != null) {
      var positions = json(alice, "/organizations/" + nodeId + "/positions");
      var id = find(positions, position).get("id").asText();
      positionIds = "[\"" + id + "\"]";
    }
    return read(body(
            alice,
            HttpMethod.POST,
            "/people",
            ("{\"firstName\":\"%s\",\"lastName\":\"%s\",\"organizationId\":\"%s\","
                    + "\"positionIds\":%s%s}")
                .formatted(
                    first,
                    last,
                    nodeId,
                    positionIds,
                    birthdate == null ? "" : ",\"birthdate\":\"" + birthdate + "\"")))
        .asText();
  }

  /** Gives {@code subject} a position with {@code roleId} on {@code nodeId}, through a person. */
  void holdAccess(String subject, String nodeId, Object roleId) {
    var position =
        read(body(
                alice,
                HttpMethod.POST,
                "/organizations/" + churchId + "/positions",
                "{\"name\":\"Cargo %s\",\"roleIds\":[\"%s\"]}"
                    .formatted(UUID.randomUUID(), roleId)))
            .get("id")
            .asText();
    var personId =
        read(body(
                alice,
                HttpMethod.POST,
                "/people",
                ("{\"firstName\":\"Persona\",\"lastName\":\"Cargo\",\"organizationId\":\"%s\","
                        + "\"positionIds\":[\"%s\"]}")
                    .formatted(nodeId, position)))
            .asText();
    link(subject, personId);
  }

  void link(String subject, String personId) {
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

  String signUp(String name) {
    var subject = "auth0|" + name + "-" + UUID.randomUUID();
    syncCurrentUserCommandHandler.handle(new SyncCurrentUserCommand(subject, email(subject), true));
    return subject;
  }

  static String email(String subject) {
    return subject.replace("auth0|", "") + "@example.org";
  }

  static JsonNode find(JsonNode array, String name) {
    for (var node : array) {
      if (name.equals(node.get("name").asText())) {
        return node;
      }
    }
    throw new AssertionError("No element named " + name + " in " + array);
  }

  static List<String> values(JsonNode array) {
    var values = new ArrayList<String>();
    array.forEach(node -> values.add(node.asText()));
    return values;
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
