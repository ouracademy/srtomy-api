package org.ouracademy.ovejafiel.attendance;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.notification.SentEmails;
import org.ouracademy.ovejafiel.notification.TestEmailConfig;
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
 * Attendance end to end over the real schema: a service without registration, visitors, the QR the
 * usher shows, the QR of a ticket and the visitors who give no name.
 */
@SpringBootTest(classes = OvejafielApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({TestSecurityConfig.class, TestEmailConfig.class})
class AttendanceQrIntegrationTests {

  static final ObjectMapper JSON = new ObjectMapper();

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  @Autowired LinkUserToPersonCommandHandler linkUserToPersonCommandHandler;
  @Autowired SentEmails sentEmails;

  RestTestClient client;
  String alice;
  String bob;
  String carol;
  String dan;
  String churchId;
  String location;
  String group;
  String serviceId;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port + "/v1").build();
    sentEmails.reset();
    alice = signUp("alice", true);
    bob = signUp("bob", true);
    carol = signUp("carol", true);
    dan = signUp("dan", true);
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
    group = createGroup("Jóvenes");
    serviceId = createEvent("Culto dominical", false, onNow());
  }

  // --- a service without registration

  @Test
  void aServiceWithoutRegistrationMarksPeopleVisitorsAndAnonymousGuests() {
    var visitor =
        read(
            body(
                alice,
                HttpMethod.POST,
                "/events/" + serviceId + "/attendance/visitors",
                "{\"firstName\":\"Rosa\",\"lastName\":\"Díaz\",\"phoneNumber\":\"999\"}"));
    var personId = visitor.get("personId").asText();
    assertThat(stateOf(personId)).isEqualTo("VISITOR");

    var guests =
        read(
            body(
                alice,
                HttpMethod.PUT,
                "/events/" + serviceId + "/attendance/guests",
                "{\"guestCount\":5}"));
    assertThat(guests.get("guestCount").asInt()).isEqualTo(5);

    var attendance = json(alice, "/events/" + serviceId + "/attendance");
    assertThat(attendance.get("personIds")).extracting(JsonNode::asText).containsExactly(personId);
    assertThat(attendance.get("guestCount").asInt()).isEqualTo(5);
    assertThat(attendance.get("marks").get(0).get("source").asText()).isEqualTo("MANUAL");
  }

  @Test
  void aNewVisitorStartsAsAVisitorInTheNodeOfTheEventAndTheStateCanGrow() {
    var personId =
        read(body(
                alice,
                HttpMethod.POST,
                "/events/" + serviceId + "/attendance/visitors",
                "{\"firstName\":\"Rosa\"}"))
            .get("personId")
            .asText();

    assertThat(stateOf(personId)).isEqualTo("VISITOR");

    body(
        alice,
        HttpMethod.PUT,
        "/people/" + personId,
        ("{\"firstName\":\"Rosa\",\"lastName\":\"Díaz\",\"organizationId\":\"%s\","
                + "\"memberState\":\"MEMBERSHIP_IN_PROGRESS\"}")
            .formatted(group));
    assertThat(stateOf(personId)).isEqualTo("MEMBERSHIP_IN_PROGRESS");
  }

  @Test
  void aPositionMakesAPersonAMemberWhateverTheStateAsked() {
    var position =
        read(body(
                alice,
                HttpMethod.POST,
                "/organizations/" + churchId + "/positions",
                "{\"name\":\"Ujier %s\",\"roleIds\":[]}".formatted(UUID.randomUUID())))
            .get("id")
            .asText();
    var personId =
        read(body(
                alice,
                HttpMethod.POST,
                "/people",
                ("{\"firstName\":\"Luis\",\"lastName\":\"Rey\",\"organizationId\":\"%s\","
                        + "\"positionIds\":[\"%s\"],\"memberState\":\"VISITOR\"}")
                    .formatted(group, position)))
            .asText();

    assertThat(stateOf(personId)).isEqualTo("MEMBER");
  }

  @Test
  void onlyWhoTakesAttendanceMarksOthersOrShowsTheQr() {
    holdAccess(bob, group, PredefinedRoles.MEMBER.value());
    var uri = "/events/" + serviceId + "/attendance";

    assertThat(status(bob, HttpMethod.GET, uri + "/qr")).isEqualTo(403);
    assertThat(status(bob, HttpMethod.POST, uri + "/visitors", "{\"firstName\":\"X\"}"))
        .isEqualTo(403);
    assertThat(status(bob, HttpMethod.PUT, uri + "/guests", "{\"guestCount\":1}")).isEqualTo(403);
    assertThat(status(bob, HttpMethod.POST, uri + "/check-in", "{\"ticketCode\":\"x\"}"))
        .isEqualTo(403);
    assertThat(status(alice, HttpMethod.PUT, uri + "/guests", "{\"guestCount\":-1}"))
        .isEqualTo(400);
  }

  // --- the QR of the event

  @Test
  void theQrOfTheEventMarksWhoScansItWithOrWithoutAnAccount() {
    holdAccess(bob, group, PredefinedRoles.MEMBER.value());
    var qr = qr(serviceId);
    var welcome = publicGet("/public/attendance/" + qr);
    assertThat(welcome.get("title").asText()).isEqualTo("Culto dominical");
    assertThat(welcome.get("status").asText()).isEqualTo("OPEN");

    // Bob is a person of the church: he is marked there, and scanning again changes nothing.
    var first = read(bodyAs(bob, "/public/attendance/" + qr, "{}"));
    assertThat(first.get("kind").asText()).isEqualTo("PERSON");
    assertThat(first.get("alreadyMarked").asBoolean()).isFalse();
    assertThat(read(bodyAs(bob, "/public/attendance/" + qr, "{}")).get("alreadyMarked").asBoolean())
        .isTrue();

    // Someone without an account who gives a name becomes a visitor; without a name, a count.
    var named = read(postPublic("/public/attendance/" + qr, "{\"name\":\"Rosa Díaz\"}"));
    assertThat(named.get("kind").asText()).isEqualTo("VISITOR");
    var nameless = read(postPublic("/public/attendance/" + qr, null));
    assertThat(nameless.get("kind").asText()).isEqualTo("ANONYMOUS");
    postPublic("/public/attendance/" + qr, "{\"name\":\" \"}");

    var attendance = json(alice, "/events/" + serviceId + "/attendance");
    assertThat(attendance.get("guestCount").asInt()).isEqualTo(2);
    assertThat(attendance.get("personIds")).hasSize(2);
    assertThat(attendance.get("marks"))
        .extracting(mark -> mark.get("source").asText())
        .containsOnly("QR_EVENT");
  }

  @Test
  void anAccountWithoutAPersonBecomesTheVisitorItNamedAndIsNotCountedTwice() {
    var qr = qr(serviceId);

    var first = read(bodyAs(carol, "/public/attendance/" + qr, "{\"name\":\"Carol Paz\"}"));
    assertThat(first.get("kind").asText()).isEqualTo("VISITOR");
    var second = read(bodyAs(carol, "/public/attendance/" + qr, "{\"name\":\"Carol Paz\"}"));

    assertThat(second.get("kind").asText()).isEqualTo("PERSON");
    assertThat(second.get("alreadyMarked").asBoolean()).isTrue();
    assertThat(json(alice, "/events/" + serviceId + "/attendance").get("personIds")).hasSize(1);
  }

  @Test
  void aCodeThatWasNotMadeByUsOrIsForAnEventNotOnNowIsRefused() {
    var qr = qr(serviceId);

    assertThat(statusPublic(HttpMethod.GET, "/public/attendance/" + qr + "x")).isEqualTo(404);
    assertThat(statusPublic(HttpMethod.POST, "/public/attendance/nonsense")).isEqualTo(404);

    var later =
        createEvent(
            "Retiro",
            false,
            "\"startTime\":\"2030-01-05T10:00:00-05:00\",\"endTime\":\"2030-01-05T12:00:00-05:00\"");
    var laterQr = qr(later);
    assertThat(publicGet("/public/attendance/" + laterQr).get("status").asText())
        .isEqualTo("OUT_OF_WINDOW");
    assertThat(statusPublic(HttpMethod.POST, "/public/attendance/" + laterQr)).isEqualTo(400);
  }

  // --- the QR of the ticket

  @Test
  void theUsherScansTheTicketAndTheSignUpIsCheckedInOnlyOnce() {
    holdAccess(bob, group, PredefinedRoles.MEMBER.value());
    var course = createEvent("Curso", true, onNow());
    var slug = json(alice, "/events/" + course + "/sharing").get("slug").asText();
    body(
        alice,
        HttpMethod.PUT,
        "/events/" + course + "/sharing",
        "{\"generalAccess\":\"ANYONE_WITH_LINK\"}");
    body(
        alice,
        HttpMethod.PUT,
        "/events/" + course + "/registration",
        "{\"requiresAccount\":false,"
            + "\"tickets\":[{\"name\":\"General\",\"priceCents\":0,\"quantity\":2}]}");
    var ticket =
        json(alice, "/events/" + course + "/registration").get("tickets").get(0).get("id").asText();
    var mine =
        read(
            bodyAs(
                bob,
                "/public/events/" + slug + "/registrations",
                registrationBody(ticket, email(bob))));
    var cancelled =
        read(
            postPublic(
                "/public/events/" + slug + "/registrations",
                registrationBody(ticket, "gone@example.org")));
    client
        .delete()
        .uri("/public/registrations/" + cancelled.get("cancelCode").asText())
        .exchange()
        .expectStatus()
        .isNoContent();
    var uri = "/events/" + course + "/attendance/check-in";

    var first =
        read(
            bodyAs(
                alice, uri, "{\"ticketCode\":\"%s\"}".formatted(mine.get("ticketCode").asText())));
    assertThat(first.get("alreadyCheckedIn").asBoolean()).isFalse();
    assertThat(first.get("ticketName").asText()).isEqualTo("General");
    var again =
        read(
            bodyAs(
                alice, uri, "{\"ticketCode\":\"%s\"}".formatted(mine.get("ticketCode").asText())));
    assertThat(again.get("alreadyCheckedIn").asBoolean()).isTrue();
    assertThat(again.get("checkedInAt").asText()).isEqualTo(first.get("checkedInAt").asText());

    // The person bob is in the church is marked as well, and the list shows who came.
    var attendance = json(alice, "/events/" + course + "/attendance");
    assertThat(attendance.get("marks").get(0).get("source").asText()).isEqualTo("QR_TICKET");
    var registrations = json(alice, "/events/" + course + "/registrations");
    assertThat(registrations.get(0).get("checkedInAt").isNull()).isFalse();
    assertThat(json(alice, "/events/" + course + "/registration").get("checkedIn").asInt())
        .isEqualTo(1);

    // Whoever gave up the seat cannot come in; a code of nothing, and the cancel code, are not
    // tickets.
    assertThat(
            statusAs(
                alice,
                uri,
                "{\"ticketCode\":\"%s\"}".formatted(cancelled.get("ticketCode").asText())))
        .isEqualTo(400);
    assertThat(statusAs(alice, uri, "{\"ticketCode\":\"nope\"}")).isEqualTo(404);
    assertThat(
            statusAs(
                alice, uri, "{\"ticketCode\":\"%s\"}".formatted(mine.get("cancelCode").asText())))
        .isEqualTo(404);

    // Bob sees it as a course he went to.
    var events = json(bob, "/me/events");
    assertThat(events.get(0).get("attended").asBoolean()).isTrue();
    assertThat(events.get(0).get("ticketCode").asText()).isEqualTo(mine.get("ticketCode").asText());
  }

  @Test
  void scanningTheQrOfTheEventAlsoChecksInTheTicketOfTheAccount() {
    var course = createEvent("Curso", true, onNow());
    var slug = json(alice, "/events/" + course + "/sharing").get("slug").asText();
    body(
        alice,
        HttpMethod.PUT,
        "/events/" + course + "/sharing",
        "{\"generalAccess\":\"ANYONE_WITH_LINK\"}");
    body(
        alice,
        HttpMethod.PUT,
        "/events/" + course + "/registration",
        "{\"requiresAccount\":false," + "\"tickets\":[{\"name\":\"General\",\"priceCents\":0}]}");
    var ticket =
        json(alice, "/events/" + course + "/registration").get("tickets").get(0).get("id").asText();
    bodyAs(
        carol, "/public/events/" + slug + "/registrations", registrationBody(ticket, email(carol)));

    var scan = read(bodyAs(carol, "/public/attendance/" + qr(course), "{}"));

    assertThat(scan.get("kind").asText()).isEqualTo("REGISTRATION");
    assertThat(
            json(alice, "/events/" + course + "/registrations").get(0).get("checkedInAt").isNull())
        .isFalse();
    assertThat(json(carol, "/me/events").get(0).get("attended").asBoolean()).isTrue();
  }

  @Test
  void anUsherMarksASignUpByHandAndTakesTheMarkBack() {
    var course = createEvent("Curso", true, onNow());
    var slug = json(alice, "/events/" + course + "/sharing").get("slug").asText();
    body(
        alice,
        HttpMethod.PUT,
        "/events/" + course + "/sharing",
        "{\"generalAccess\":\"ANYONE_WITH_LINK\"}");
    body(
        alice,
        HttpMethod.PUT,
        "/events/" + course + "/registration",
        "{\"requiresAccount\":false," + "\"tickets\":[{\"name\":\"General\",\"priceCents\":0}]}");
    var ticket =
        json(alice, "/events/" + course + "/registration").get("tickets").get(0).get("id").asText();
    postPublic(
        "/public/events/" + slug + "/registrations", registrationBody(ticket, "ana@example.org"));
    var id = json(alice, "/events/" + course + "/registrations").get(0).get("id").asText();
    var uri = "/events/" + course + "/registrations/" + id + "/check-in";

    var marked = read(body(alice, HttpMethod.PUT, uri, "{\"checkedIn\":true}"));
    assertThat(marked.get("checkedInAt").isNull()).isFalse();
    assertThat(
            json(alice, "/events/" + course + "/registrations")
                .get(0)
                .get("checkInSource")
                .asText())
        .isEqualTo("MANUAL");

    var undone = read(body(alice, HttpMethod.PUT, uri, "{\"checkedIn\":false}"));
    assertThat(undone.get("checkedInAt").isNull()).isTrue();
  }

  // --- helpers specific to attendance

  static String onNow() {
    var start = java.time.OffsetDateTime.now().minusMinutes(20);
    return "\"startTime\":\"%s\",\"endTime\":\"%s\"".formatted(start, start.plusHours(2));
  }

  String qr(String eventId) {
    return json(alice, "/events/" + eventId + "/attendance/qr").get("token").asText();
  }

  String stateOf(String personId) {
    for (var person : json(alice, "/people?organizationId=" + group + "&size=100").get("content")) {
      if (person.get("id").asText().equals(personId)) {
        return person.get("memberState").asText();
      }
    }
    return null;
  }

  String registrationBody(String ticketId, String email) {
    return "{\"ticketTypeId\":\"%s\",\"firstName\":\"Nombre\",\"lastName\":\"Apellido\",\"email\":\"%s\"}"
        .formatted(ticketId, email);
  }

  JsonNode publicGet(String uri) {
    return read(
        client
            .get()
            .uri(uri)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody());
  }

  int statusPublic(HttpMethod method, String uri) {
    return client.method(method).uri(uri).exchange().returnResult(String.class).getStatus().value();
  }

  String postPublic(String uri, String json) {
    var request = client.post().uri(uri);
    if (json != null) {
      request.contentType(MediaType.APPLICATION_JSON).body(json);
    }
    return request
        .exchange()
        .expectStatus()
        .is2xxSuccessful()
        .expectBody(String.class)
        .returnResult()
        .getResponseBody();
  }

  int statusAs(String subject, String uri, String body) {
    return statusAs(subject, HttpMethod.POST, uri, body);
  }

  String createEvent(String name, boolean course, String times) {
    return read(body(
            alice,
            HttpMethod.POST,
            "/events",
            ("{\"name\":\"%s\",%s,\"organizationId\":\"%s\",\"isACourse\":%s}")
                .formatted(name, times, group, course)))
        .asText();
  }

  // --- generic helpers

  String createGroup(String name) {
    return read(body(
            alice,
            HttpMethod.POST,
            "/organizations/locations/childs",
            "{\"name\":\"%s\",\"type\":\"GROUP\",\"parentId\":\"%s\"}".formatted(name, location)))
        .get("id")
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
    return status(subject, method, uri, null);
  }

  int status(String subject, HttpMethod method, String uri, String body) {
    return statusAs(subject, method, uri, body);
  }

  int statusAs(String subject, HttpMethod method, String uri, String body) {
    var request = client.method(method).uri(uri).headers(h -> h.setBearerAuth(token(subject)));
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).body(body);
    }
    return request.exchange().returnResult(String.class).getStatus().value();
  }

  String bodyAs(String subject, String uri, String body) {
    return body(subject, HttpMethod.POST, uri, body);
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
