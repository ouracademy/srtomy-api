package org.ouracademy.srtomy.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.notification.SentEmails;
import org.ouracademy.srtomy.notification.TestEmailConfig;
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
 * Invitations end to end over the real schema: who may invite, the link in the email, what
 * accepting does (links the user to a person and gives the position) and the ways it is refused.
 */
@SpringBootTest(classes = SrtomyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({TestSecurityConfig.class, TestEmailConfig.class})
class InvitationIntegrationTests {

  static final ObjectMapper JSON = new ObjectMapper();
  static final Pattern LINK = Pattern.compile("/invitacion/([A-Za-z0-9_-]+)");

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  @Autowired SentEmails sentEmails;

  RestTestClient client;
  String alice;
  String bob;
  String carol;
  Church church;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port + "/v1").build();
    sentEmails.reset();
    alice = signUp("alice", true);
    bob = signUp("bob", true);
    carol = signUp("carol", true);
    church = createChurch(alice);
  }

  @Test
  void invitingByEmailAndAcceptingLinksTheUserAndGivesThePosition() {
    var usher = createPosition(alice, church.id, "Ujier", PredefinedRoles.COLLABORATOR.value());

    var created = invite(alice, church.location, email(bob), usher, "Beto", "Pérez");
    assertThat(created.get("emailSent").asBoolean()).isTrue();
    assertThat(created.get("invitation").get("emailStatus").asText()).isEqualTo("SENT");
    var token = lastToken();
    assertThat(json(alice, "/organizations/" + church.location + "/invitations").size())
        .isEqualTo(1);
    // The token is only in the email, never in what the API returns.
    assertThat(created.toString()).doesNotContain(token);

    // Before accepting, the invited has no access.
    assertThat(status(bob, HttpMethod.GET, "/organizations/" + church.location)).isEqualTo(403);

    var accepted = read(body(bob, HttpMethod.POST, "/invitations/" + token + "/accept", null));
    assertThat(accepted.get("organizationId").asText()).isEqualTo(church.location);
    assertThat(accepted.get("churchId").asText()).isEqualTo(church.id);

    assertThat(permissions(bob, church.location))
        .contains("org.read", "events.read", "attendance.write")
        .doesNotContain("iam.manage", "invitations.send");
    var person = json(alice, "/people/" + accepted.get("personId").asText());
    assertThat(person.get("firstName").asText()).isEqualTo("Beto");
    assertThat(person.get("email").asText()).isEqualTo(email(bob));
    assertThat(json(alice, "/organizations/" + church.location + "/invitations")).isEmpty();
    // A used link does not work again.
    assertThat(status(bob, HttpMethod.POST, "/invitations/" + token + "/accept")).isEqualTo(400);
  }

  @Test
  void anExistingPersonOfTheGroupIsLinkedInsteadOfCreatingAnotherOne() {
    var group = createGroup(alice, church.location);
    var personId = createPerson(alice, "Lucía", group);

    var created = invite(alice, group, email(bob), null, personId);
    var token = lastToken();
    assertThat(created.get("invitation").get("personId").asText()).isEqualTo(personId);

    var accepted = read(body(bob, HttpMethod.POST, "/invitations/" + token + "/accept", null));

    assertThat(accepted.get("personId").asText()).isEqualTo(personId);
    // An account without a position gives no access of its own.
    assertThat(status(bob, HttpMethod.GET, "/organizations/" + group)).isEqualTo(403);
    // The person now has an account, so inviting them again is refused.
    assertThat(
            status(
                alice,
                HttpMethod.POST,
                "/organizations/" + group + "/invitations",
                "{\"email\":\"%s\",\"personId\":\"%s\"}".formatted(email(carol), personId)))
        .isEqualTo(400);
  }

  @Test
  void onlyTheInvitedVerifiedEmailCanAccept() {
    invite(alice, church.location, email(bob), null, "Beto", "Pérez");
    var token = lastToken();

    assertThat(status(carol, HttpMethod.POST, "/invitations/" + token + "/accept")).isEqualTo(403);

    // Same email but not verified by the identity provider.
    syncCurrentUserCommandHandler.handle(new SyncCurrentUserCommand(bob, email(bob), false));
    assertThat(status(bob, HttpMethod.POST, "/invitations/" + token + "/accept")).isEqualTo(403);

    syncCurrentUserCommandHandler.handle(new SyncCurrentUserCommand(bob, email(bob), true));
    assertThat(status(bob, HttpMethod.POST, "/invitations/" + token + "/accept")).isEqualTo(200);
  }

  @Test
  void aLeaderInvitesOnlyWhatTheyCouldGiveThemselves() {
    var leader = createPosition(alice, church.id, "Líder", PredefinedRoles.LEADER.value());
    var admin = createPosition(alice, church.id, "Admin", PredefinedRoles.ADMIN.value());
    var label = createPosition(alice, church.id, "Hermano", null);
    var usher = createPosition(alice, church.id, "Ujier", PredefinedRoles.COLLABORATOR.value());
    var group = createGroup(alice, church.location);
    // Bob leads the group with the Leader level: he can invite, but cannot manage access.
    invite(alice, group, email(bob), leader, "Beto", "Pérez");
    body(bob, HttpMethod.POST, "/invitations/" + lastToken() + "/accept", null);
    assertThat(permissions(bob, group)).contains("invitations.send").doesNotContain("iam.manage");

    assertThat(inviteStatus(bob, group, email(carol), usher)).isEqualTo(201);
    assertThat(inviteStatus(bob, group, email(carol), label)).isEqualTo(201);
    assertThat(inviteStatus(bob, group, email(carol), leader)).isEqualTo(201);
    // He holds no iam.manage, which Administrator carries.
    assertThat(inviteStatus(bob, group, email(carol), admin)).isEqualTo(403);
    // And his reach ends at his group.
    assertThat(inviteStatus(bob, church.location, email(carol), usher)).isEqualTo(403);
  }

  @Test
  void aLeaderOfAGroupCanInviteSomeoneWhoIsOnlyLeaderThere() {
    // The leader belongs to the location, not to the group, and leads the group.
    var location = church.location;
    var leaderId = createPerson(alice, "Lucía", location);
    var group =
        read(body(
                alice,
                HttpMethod.POST,
                "/organizations/locations/childs",
                "{\"name\":\"Jóvenes\",\"type\":\"GROUP\",\"parentId\":\"%s\",\"leaderIds\":[\"%s\"]}"
                    .formatted(location, leaderId)))
            .get("id")
            .asText();

    invite(alice, group, email(bob), null, leaderId);
    body(bob, HttpMethod.POST, "/invitations/" + lastToken() + "/accept", null);

    assertThat(permissions(bob, group)).contains("members.write", "invitations.send");
    assertThat(permissions(bob, location)).isEmpty();
  }

  @Test
  void nobodyLinksAnAccountToAPersonWhoHoldsMoreAccessThanTheyCanGive() {
    var leader = createPosition(alice, church.id, "Líder", PredefinedRoles.LEADER.value());
    var admin = createPosition(alice, church.id, "Admin", PredefinedRoles.ADMIN.value());
    var group = createGroup(alice, church.location);
    invite(alice, group, email(bob), leader, "Beto", "Pérez");
    body(bob, HttpMethod.POST, "/invitations/" + lastToken() + "/accept", null);
    // A member of the group who is also an Administrator.
    var adminPerson =
        read(body(
                alice,
                HttpMethod.POST,
                "/people",
                "{\"firstName\":\"Ana\",\"lastName\":\"Jefa\",\"organizationId\":\"%s\",\"positionIds\":[\"%s\"]}"
                    .formatted(group, admin)))
            .asText();

    // Bob leads the group but cannot hand over an Administrator's access.
    assertThat(
            status(
                bob,
                HttpMethod.POST,
                "/organizations/" + group + "/invitations",
                "{\"email\":\"%s\",\"personId\":\"%s\"}".formatted(email(carol), adminPerson)))
        .isEqualTo(403);
    assertThat(
            status(
                alice,
                HttpMethod.POST,
                "/organizations/" + group + "/invitations",
                "{\"email\":\"%s\",\"personId\":\"%s\"}".formatted(email(carol), adminPerson)))
        .isEqualTo(201);
  }

  @Test
  void aCollaboratorCannotInviteNorList() {
    var usher = createPosition(alice, church.id, "Ujier", PredefinedRoles.COLLABORATOR.value());
    invite(alice, church.location, email(bob), usher, "Beto", "Pérez");
    body(bob, HttpMethod.POST, "/invitations/" + lastToken() + "/accept", null);

    assertThat(inviteStatus(bob, church.location, email(carol), null)).isEqualTo(403);
    assertThat(status(bob, HttpMethod.GET, "/organizations/" + church.location + "/invitations"))
        .isEqualTo(403);
  }

  @Test
  void aPositionOfAnotherChurchIsRejected() {
    var other = createChurch(carol);
    var foreign = createPosition(carol, other.id, "Ajeno", null);

    assertThat(inviteStatus(alice, church.location, email(bob), foreign)).isEqualTo(400);
  }

  @Test
  void aRevokedInvitationCannotBeAccepted() {
    var created = invite(alice, church.location, email(bob), null, "Beto", "Pérez");
    var token = lastToken();

    assertThat(
            status(
                alice,
                HttpMethod.DELETE,
                "/invitations/" + created.get("invitation").get("id").asText()))
        .isEqualTo(204);

    assertThat(preview(token).get("status").asText()).isEqualTo("REVOKED");
    assertThat(status(bob, HttpMethod.POST, "/invitations/" + token + "/accept")).isEqualTo(400);
  }

  @Test
  void invitingAgainReplacesThePreviousLink() {
    invite(alice, church.location, email(bob), null, "Beto", "Pérez");
    var first = lastToken();
    invite(alice, church.location, email(bob), null, "Beto", "Pérez");
    var second = lastToken();

    assertThat(second).isNotEqualTo(first);
    assertThat(json(alice, "/organizations/" + church.location + "/invitations").size())
        .isEqualTo(1);
    assertThat(status(bob, HttpMethod.POST, "/invitations/" + first + "/accept")).isEqualTo(400);
    assertThat(status(bob, HttpMethod.POST, "/invitations/" + second + "/accept")).isEqualTo(200);
  }

  @Test
  void thePreviewNeedsNoSignInAndAnUnknownTokenIsNotFound() {
    invite(alice, church.location, email(bob), null, "Beto", "Pérez");
    var token = lastToken();

    var preview =
        read(
            client
                .get()
                .uri("/public/invitations/" + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody());

    assertThat(preview.get("email").asText()).isEqualTo(email(bob));
    assertThat(preview.get("organizationName").asText()).isEqualTo("Sede");
    assertThat(preview.get("status").asText()).isEqualTo("PENDING");
    client.get().uri("/public/invitations/nope").exchange().expectStatus().isNotFound();
    // Everything else still asks for a token.
    client.get().uri("/me/invitations").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void theSignedInUserSeesAndAcceptsTheirPendingInvitations() {
    invite(alice, church.location, email(bob), null, "Beto", "Pérez");

    var mine = json(bob, "/me/invitations");
    assertThat(mine.size()).isEqualTo(1);
    assertThat(json(carol, "/me/invitations")).isEmpty();

    assertThat(
            status(
                carol,
                HttpMethod.POST,
                "/me/invitations/" + mine.get(0).get("id").asText() + "/accept"))
        .isEqualTo(403);
    assertThat(
            status(
                bob,
                HttpMethod.POST,
                "/me/invitations/" + mine.get(0).get("id").asText() + "/accept"))
        .isEqualTo(200);
    assertThat(json(bob, "/me/invitations")).isEmpty();
  }

  @Test
  void anInvitationNeedsAValidEmail() {
    assertThat(
            status(
                alice,
                HttpMethod.POST,
                "/organizations/" + church.location + "/invitations",
                "{\"email\":\"not-an-email\"}"))
        .isEqualTo(400);
  }

  @Test
  void invitingByEmailAloneLetsWhoAcceptsSayTheirName() {
    var created =
        read(
            body(
                alice,
                HttpMethod.POST,
                "/organizations/" + church.location + "/invitations",
                "{\"email\":\"%s\"}".formatted(email(bob))));
    var token = lastToken();
    assertThat(created.get("invitation").get("needsName").asBoolean()).isTrue();
    assertThat(preview(token).get("needsName").asBoolean()).isTrue();

    // Without a name the invitation cannot be accepted.
    assertThat(status(bob, HttpMethod.POST, "/invitations/" + token + "/accept")).isEqualTo(400);
    var accepted =
        read(
            body(
                bob,
                HttpMethod.POST,
                "/invitations/" + token + "/accept",
                "{\"firstName\":\"Beto\",\"lastName\":\"Pérez\"}"));

    var person = json(alice, "/people/" + accepted.get("personId").asText());
    assertThat(person.get("firstName").asText()).isEqualTo("Beto");
    assertThat(person.get("email").asText()).isEqualTo(email(bob));
  }

  @Test
  void theEmailOfAnExistingPersonLinksThatPersonInsteadOfCreatingAnother() {
    var group = createGroup(alice, church.location);
    var personId =
        read(body(
                alice,
                HttpMethod.POST,
                "/people",
                ("{\"firstName\":\"Lucía\",\"lastName\":\"Prueba\","
                        + "\"email\":\"%s\",\"organizationId\":\"%s\"}")
                    .formatted(email(bob), group)))
            .asText();

    var created =
        read(
            body(
                alice,
                HttpMethod.POST,
                "/organizations/" + group + "/invitations",
                "{\"email\":\"%s\"}".formatted(email(bob).toUpperCase())));

    assertThat(created.get("invitation").get("personId").asText()).isEqualTo(personId);
    assertThat(created.get("invitation").get("needsName").asBoolean()).isFalse();
    var accepted =
        read(body(bob, HttpMethod.POST, "/invitations/" + lastToken() + "/accept", null));
    assertThat(accepted.get("personId").asText()).isEqualTo(personId);
  }

  @Test
  void twoPeopleWithTheSameEmailAskToChoose() {
    var group = createGroup(alice, church.location);
    for (var name : new String[] {"Ana", "Eva"}) {
      body(
          alice,
          HttpMethod.POST,
          "/people",
          ("{\"firstName\":\"%s\",\"lastName\":\"Prueba\",\"email\":\"%s\","
                  + "\"organizationId\":\"%s\"}")
              .formatted(name, email(bob), group));
    }

    assertThat(
            status(
                alice,
                HttpMethod.POST,
                "/organizations/" + group + "/invitations",
                "{\"email\":\"%s\"}".formatted(email(bob))))
        .isEqualTo(400);
  }

  @Test
  void invitingAPersonWithoutEmailGivesThemTheInvitedOne() {
    var group = createGroup(alice, church.location);
    var personId = createPerson(alice, "Lucía", group);

    invite(alice, group, email(carol), null, personId);

    assertThat(json(alice, "/people/" + personId).get("email").asText()).isEqualTo(email(carol));
  }

  @Test
  void aPersonWithAnotherEmailKeepsIt() {
    var group = createGroup(alice, church.location);
    var personId =
        read(body(
                alice,
                HttpMethod.POST,
                "/people",
                ("{\"firstName\":\"Lucía\",\"lastName\":\"Prueba\","
                        + "\"email\":\"personal@example.org\",\"organizationId\":\"%s\"}")
                    .formatted(group)))
            .asText();

    invite(alice, group, email(carol), null, personId);

    assertThat(json(alice, "/people/" + personId).get("email").asText())
        .isEqualTo("personal@example.org");
  }

  @Test
  void aFailedEmailIsReportedAndTheInvitationRemains() {
    sentEmails.failWith(new IllegalStateException("403 from the provider"));

    var created = invite(alice, church.location, email(bob), null, "Beto", "Pérez");

    assertThat(created.get("emailSent").asBoolean()).isFalse();
    // The email waits for its retry; the invitation shows it.
    assertThat(created.get("invitation").get("emailStatus").asText()).isEqualTo("PENDING");
    var pending = json(alice, "/organizations/" + church.location + "/invitations");
    assertThat(pending.size()).isEqualTo(1);
    assertThat(pending.get(0).get("emailStatus").asText()).isEqualTo("PENDING");
  }

  @Test
  void aPersonCanBeCreatedWithAnEmail() {
    var id =
        read(body(
                alice,
                HttpMethod.POST,
                "/people",
                "{\"firstName\":\"Ana\",\"lastName\":\"Ruiz\",\"email\":\"ana@example.org\",\"organizationId\":\"%s\"}"
                    .formatted(church.location)))
            .asText();

    assertThat(json(alice, "/people/" + id).get("email").asText()).isEqualTo("ana@example.org");
    assertThat(
            status(
                alice,
                HttpMethod.POST,
                "/people",
                "{\"firstName\":\"Ana\",\"lastName\":\"Ruiz\",\"email\":\"nope\",\"organizationId\":\"%s\"}"
                    .formatted(church.location)))
        .isEqualTo(400);
  }

  // --- helpers

  record Church(String id, String location) {}

  JsonNode preview(String token) {
    return read(
        client
            .get()
            .uri("/public/invitations/" + token)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody());
  }

  /** The token of the link in the last email sent. */
  String lastToken() {
    var matcher = LINK.matcher(sentEmails.last().text());
    assertThat(matcher.find()).as("a link in the email").isTrue();
    return matcher.group(1);
  }

  JsonNode invite(
      String subject, String nodeId, String email, String positionId, String first, String last) {
    return read(
        body(
            subject,
            HttpMethod.POST,
            "/organizations/" + nodeId + "/invitations",
            "{\"email\":\"%s\",\"positionId\":%s,\"firstName\":\"%s\",\"lastName\":\"%s\"}"
                .formatted(
                    email, positionId == null ? "null" : "\"" + positionId + "\"", first, last)));
  }

  JsonNode invite(String subject, String nodeId, String email, String positionId, String personId) {
    return read(
        body(
            subject,
            HttpMethod.POST,
            "/organizations/" + nodeId + "/invitations",
            "{\"email\":\"%s\",\"positionId\":%s,\"personId\":\"%s\"}"
                .formatted(
                    email, positionId == null ? "null" : "\"" + positionId + "\"", personId)));
  }

  int inviteStatus(String subject, String nodeId, String email, String positionId) {
    return status(
        subject,
        HttpMethod.POST,
        "/organizations/" + nodeId + "/invitations",
        "{\"email\":\"%s\",\"positionId\":%s,\"firstName\":\"Nuevo\",\"lastName\":\"Invitado\"}"
            .formatted(email, positionId == null ? "null" : "\"" + positionId + "\""));
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
    return read(body(
            subject,
            HttpMethod.POST,
            "/organizations/locations/childs",
            "{\"name\":\"Grupo\",\"type\":\"GROUP\",\"parentId\":\"%s\"}".formatted(parentId)))
        .get("id")
        .asText();
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

  String createPerson(String subject, String firstName, String organizationId) {
    return read(body(
            subject,
            HttpMethod.POST,
            "/people",
            "{\"firstName\":\"%s\",\"lastName\":\"Prueba\",\"organizationId\":\"%s\"}"
                .formatted(firstName, organizationId)))
        .asText();
  }

  List<String> permissions(String subject, String organizationId) {
    var values = new ArrayList<String>();
    json(subject, "/me/permissions?organizationId=" + organizationId)
        .forEach(node -> values.add(node.asText()));
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
