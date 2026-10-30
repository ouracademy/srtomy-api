package org.ouracademy.ovejafiel.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Composite events over the real schema: a three-day mega event with sub-events, the registration
 * the sub-events inherit (or override), the ticket check-in in a sub-event and deleting it all.
 */
@SpringBootTest(classes = OvejafielApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({TestSecurityConfig.class, TestEmailConfig.class})
class CompositeEventsIntegrationTests {

  static final ObjectMapper JSON = new ObjectMapper();

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  @Autowired LinkUserToPersonCommandHandler linkUserToPersonCommandHandler;
  @Autowired JdbcTemplate jdbc;

  RestTestClient client;
  String alice;
  String carol;
  String churchId;
  String location;
  String conference;
  String conferenceSlug;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port + "/v1").build();
    alice = signUp("alice");
    carol = signUp("carol");
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
    conference =
        createEvent("Mega evento", "2030-01-05T00:00:00Z", "2030-01-07T23:00:00Z", null, null);
    // Sub-events copy who can open the mega event when they are created.
    body(alice, HttpMethod.PUT, "/events/" + conference + "/sharing", sharing("ANYONE_WITH_LINK"));
    conferenceSlug = json(alice, "/events/" + conference + "/sharing").get("slug").asText();
  }

  @Test
  void aMegaEventListsItsSubEventsAndEachOneKnowsItsContainer() {
    var workshop = subEvent("Taller", "INHERIT");
    var dinner = subEvent("Cena", "OWN");

    var detail = json(alice, "/events/" + conference);
    assertThat(detail.get("children"))
        .extracting(child -> child.get("title").asText())
        .containsExactly("Taller", "Cena");
    assertThat(detail.get("parentEventId").isNull()).isTrue();
    var child = json(alice, "/events/" + workshop);
    assertThat(child.get("parentEventId").asText()).isEqualTo(conference);
    assertThat(child.get("parentTitle").asText()).isEqualTo("Mega evento");
    assertThat(child.get("registrationMode").asText()).isEqualTo("INHERIT");
    assertThat(json(alice, "/events/" + dinner).get("registrationMode").asText()).isEqualTo("OWN");
  }

  @Test
  void aSubEventMustFitInsideTheMegaEventAndCannotNestOrRepeat() {
    var workshop = subEvent("Taller", "INHERIT");

    assertThat(
            createEventStatus("Fuera", "2030-01-08T10:00:00Z", "2030-01-08T11:00:00Z", conference))
        .isEqualTo(400);
    assertThat(
            createEventStatus("Anidado", "2030-01-06T10:00:00Z", "2030-01-06T11:00:00Z", workshop))
        .isEqualTo(400);
    assertThat(
            createEventStatus(
                "Inexistente",
                "2030-01-06T10:00:00Z",
                "2030-01-06T11:00:00Z",
                UUID.randomUUID().toString()))
        .isEqualTo(404);
    assertThat(
            status(
                alice,
                HttpMethod.POST,
                "/events",
                eventJson("Repetido", "2030-01-06T10:00:00Z", "2030-01-06T11:00:00Z", conference)
                    .replace(
                        "}",
                        ",\"recurrence\":{\"frequency\":\"DAILY\",\"interval\":1,"
                            + "\"endType\":\"COUNT\",\"count\":2}}")))
        .isEqualTo(400);
  }

  @Test
  void theProgramOfTheSharedPageSaysWhatTheMegaTicketIncludes() {
    subEvent("Taller", "INHERIT");
    subEvent("Cena", "OWN");

    var page = publicPage(conferenceSlug);

    assertThat(page.get("program"))
        .extracting(
            item -> item.get("title").asText(), item -> item.get("includedInTicket").asBoolean())
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("Taller", true),
            org.assertj.core.groups.Tuple.tuple("Cena", false));
    assertThat(page.toString()).doesNotContain("cancelCode");
  }

  @Test
  void aSubEventThatInheritsOrIsOpenCannotBeSignedUpForButAnOwnOneCan() {
    var workshop = subEvent("Taller", "INHERIT");
    var open = subEvent("Culto", "OPEN");
    var dinner = subEvent("Cena", "OWN");
    var ticket = openRegistration(conference);
    var dinnerTicket = openRegistration(dinner);

    assertThat(registerStatus(slugOf(workshop), ticket)).isEqualTo(400);
    assertThat(registerStatus(slugOf(open), ticket)).isEqualTo(400);
    assertThat(registerStatus(slugOf(dinner), dinnerTicket)).isEqualTo(201);
    var inherited = publicPage(slugOf(workshop));
    assertThat(inherited.get("canRegister").asBoolean()).isFalse();
    assertThat(inherited.get("registration").get("tickets")).hasSize(1);
    assertThat(inherited.get("container").get("slug").asText()).isEqualTo(conferenceSlug);
    assertThat(publicPage(slugOf(open)).get("registration").isNull()).isTrue();
  }

  @Test
  void theMegaTicketChecksThePersonInTheSubEventAndLeavesTheMegaSignUpUntouched() {
    var workshop = subEvent("Taller", "INHERIT");
    var dinner = subEvent("Cena", "OWN");
    var ticket = openRegistration(conference);
    var personId = linkToAPerson(carol);
    var mine =
        read(
            body(
                carol,
                HttpMethod.POST,
                "/public/events/" + conferenceSlug + "/registrations",
                register(ticket, "carol@example.org")));
    var scan = "{\"ticketCode\":\"%s\"}".formatted(mine.get("ticketCode").asText());

    var first =
        read(body(alice, HttpMethod.POST, "/events/" + workshop + "/attendance/check-in", scan));
    assertThat(first.get("alreadyCheckedIn").asBoolean()).isFalse();
    var again =
        read(body(alice, HttpMethod.POST, "/events/" + workshop + "/attendance/check-in", scan));
    assertThat(again.get("alreadyCheckedIn").asBoolean()).isTrue();

    assertThat(json(alice, "/events/" + workshop + "/attendance").get("personIds"))
        .extracting(JsonNode::asText)
        .containsExactly(personId);
    // Nobody was marked in the mega event itself: it has no attendance at all (204).
    assertThat(status(alice, HttpMethod.GET, "/events/" + conference + "/attendance"))
        .isEqualTo(204);
    assertThat(
            json(alice, "/events/" + conference + "/registrations")
                .get(0)
                .get("checkedInAt")
                .isNull())
        .isTrue();
    // The dinner has its own registration: the mega ticket is not enough there.
    assertThat(status(alice, HttpMethod.POST, "/events/" + dinner + "/attendance/check-in", scan))
        .isEqualTo(400);
  }

  @Test
  void myEventsListTheSubEventsOfAMegaEventIHaveATicketFor() {
    var workshop = subEvent("Taller", "INHERIT");
    var dinner = subEvent("Cena", "OWN");
    var ticket = openRegistration(conference);
    body(
        carol,
        HttpMethod.POST,
        "/public/events/" + conferenceSlug + "/registrations",
        register(ticket, "carol@example.org"));

    var mine = json(carol, "/me/events");

    assertThat(mine)
        .extracting(item -> item.get("title").asText())
        .contains("Mega evento", "Taller")
        .doesNotContain("Cena");
    var inherited = find(mine, "Taller");
    assertThat(inherited.get("registered").asBoolean()).isTrue();
    assertThat(inherited.get("parentTitle").asText()).isEqualTo("Mega evento");
    assertThat(find(mine, "Mega evento").get("parentTitle").isNull()).isTrue();
    assertThat(workshop).isNotEqualTo(dinner);
  }

  @Test
  void deletingTheMegaEventDeletesItsSubEventsButDeletingOneLeavesTheMegaEvent() {
    var workshop = subEvent("Taller", "INHERIT");
    var dinner = subEvent("Cena", "OWN");

    assertThat(status(alice, HttpMethod.DELETE, "/events/" + dinner, "{\"option\":\"THIS_EVENT\"}"))
        .isEqualTo(200);
    assertThat(status(alice, HttpMethod.GET, "/events/" + conference)).isEqualTo(200);
    assertThat(json(alice, "/events/" + conference).get("children")).hasSize(1);

    assertThat(
            status(
                alice, HttpMethod.DELETE, "/events/" + conference, "{\"option\":\"THIS_EVENT\"}"))
        .isEqualTo(200);
    assertThat(status(alice, HttpMethod.GET, "/events/" + workshop)).isEqualTo(404);
    assertThat(status(alice, HttpMethod.GET, "/events/" + conference)).isEqualTo(404);
  }

  /** The PUT body that keeps the data of the event and changes its dates. */
  String editJson(String name, String start, String end, String extra) {
    return ("{\"name\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\","
            + "\"organizationId\":\"%s\",\"isACourse\":false,\"option\":\"THIS_EVENT\"%s}")
        .formatted(name, start, end, location, extra);
  }

  @Test
  void editingASubEventKeepsItInsideItsMegaEventAndLetsItChangeHowItIsEntered() {
    var workshop = subEvent("Taller", "INHERIT");
    var path = "/events/" + workshop;

    assertThat(
            status(
                alice,
                HttpMethod.PUT,
                path,
                editJson("Taller", "2030-01-08T10:00:00Z", "2030-01-08T12:00:00Z", "")))
        .isEqualTo(400);
    assertThat(
            status(
                alice,
                HttpMethod.PUT,
                path,
                editJson(
                    "Taller",
                    "2030-01-07T09:00:00Z",
                    "2030-01-07T11:00:00Z",
                    ",\"registrationMode\":\"OWN\"")))
        .isEqualTo(200);

    var edited = json(alice, path);
    assertThat(edited.get("start").asText()).startsWith("2030-01-07T09:00");
    assertThat(edited.get("registrationMode").asText()).isEqualTo("OWN");
    assertThat(edited.get("parentEventId").asText()).isEqualTo(conference);
  }

  @Test
  void aMegaEventCannotBeEditedLeavingASubEventOutNorMovedToAnotherEventNorRepeat() {
    var workshop = subEvent("Taller", "INHERIT");
    var path = "/events/" + conference;

    // The workshop is on the 6th: the mega event cannot end on the 5th.
    assertThat(
            status(
                alice,
                HttpMethod.PUT,
                path,
                editJson("Mega evento", "2030-01-05T00:00:00Z", "2030-01-05T23:00:00Z", "")))
        .isEqualTo(400);
    assertThat(
            status(
                alice,
                HttpMethod.PUT,
                path,
                editJson(
                    "Mega evento",
                    "2030-01-05T00:00:00Z",
                    "2030-01-07T23:00:00Z",
                    ",\"recurrence\":{\"frequency\":\"DAILY\",\"interval\":1,"
                        + "\"endType\":\"COUNT\",\"count\":2}")))
        .isEqualTo(400);
    assertThat(
            status(
                alice,
                HttpMethod.PUT,
                "/events/" + workshop,
                editJson(
                    "Taller",
                    "2030-01-06T10:00:00Z",
                    "2030-01-06T12:00:00Z",
                    ",\"parentEventId\":\"" + UUID.randomUUID() + "\"")))
        .isEqualTo(400);
    // It can grow, and the sub-event is still its child.
    assertThat(
            status(
                alice,
                HttpMethod.PUT,
                path,
                editJson("Mega evento", "2030-01-04T00:00:00Z", "2030-01-08T23:00:00Z", "")))
        .isEqualTo(200);
    assertThat(json(alice, path).get("children")).hasSize(1);
  }

  @Test
  void movingASubEventEmailsThePeopleOfTheMegaEventOnceAndNobodyForAnOpenOne() {
    var workshop = subEvent("Taller", "INHERIT");
    var open = subEvent("Culto", "OPEN");
    var ticket = openRegistration(conference);
    // Unique, because the outbox table keeps what earlier runs sent.
    var email = "carol-" + UUID.randomUUID() + "@example.org";
    body(
        carol,
        HttpMethod.POST,
        "/public/events/" + conferenceSlug + "/registrations",
        register(ticket, email));

    var moved =
        read(
            body(
                alice,
                HttpMethod.PUT,
                "/events/" + workshop,
                editJson("Taller", "2030-01-07T09:00:00Z", "2030-01-07T11:00:00Z", "")));
    var openMoved =
        read(
            body(
                alice,
                HttpMethod.PUT,
                "/events/" + open,
                editJson("Culto", "2030-01-07T09:00:00Z", "2030-01-07T11:00:00Z", "")));

    assertThat(moved.get("notifiedPeople").asInt()).isEqualTo(1);
    assertThat(openMoved.get("notifiedPeople").asInt()).isZero();
    assertThat(
            jdbc.queryForList(
                "select recipient from outbox_message where type = 'EVENT_RESCHEDULED' "
                    + "and recipient = ?",
                String.class,
                email))
        .hasSize(1);
  }

  JsonNode find(JsonNode list, String title) {
    for (var item : list) {
      if (item.get("title").asText().equals(title)) {
        return item;
      }
    }
    throw new AssertionError("Not in the list: " + title);
  }

  String slugOf(String eventId) {
    return json(alice, "/events/" + eventId + "/sharing").get("slug").asText();
  }

  String subEvent(String name, String mode) {
    return createEvent(name, "2030-01-06T10:00:00Z", "2030-01-06T12:00:00Z", conference, mode);
  }

  String eventJson(String name, String start, String end, String parent) {
    return "{\"name\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\",\"organizationId\":\"%s\"%s}"
        .formatted(
            name,
            start,
            end,
            location,
            parent == null ? "" : ",\"parentEventId\":\"" + parent + "\"");
  }

  String createEvent(String name, String start, String end, String parent, String mode) {
    var json = eventJson(name, start, end, parent);
    if (mode != null) {
      json = json.replace("}", ",\"registrationMode\":\"" + mode + "\"}");
    }
    return read(body(alice, HttpMethod.POST, "/events", json)).asText();
  }

  int createEventStatus(String name, String start, String end, String parent) {
    return status(alice, HttpMethod.POST, "/events", eventJson(name, start, end, parent));
  }

  /** Opens the registration of the event with one ticket and returns the ticket id. */
  String openRegistration(String eventId) {
    var ticket =
        read(body(
                alice,
                HttpMethod.PUT,
                "/events/" + eventId + "/registration",
                "{\"requiresAccount\":false,\"tickets\":["
                    + "{\"name\":\"General\",\"priceCents\":0,\"currency\":\"PEN\","
                    + "\"quantity\":10}]}"))
            .get("tickets")
            .get(0)
            .get("id")
            .asText();
    body(alice, HttpMethod.PUT, "/events/" + eventId + "/registration/open", "{\"open\":true}");
    return ticket;
  }

  String register(String ticket, String email) {
    return "{\"ticketTypeId\":\"%s\",\"firstName\":\"Carol\",\"lastName\":\"Paz\",\"email\":\"%s\"}"
        .formatted(ticket, email);
  }

  int registerStatus(String slug, String ticket) {
    return client
        .post()
        .uri("/public/events/" + slug + "/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .body(register(ticket, "anon@example.org"))
        .exchange()
        .returnResult(String.class)
        .getStatus()
        .value();
  }

  JsonNode publicPage(String slug) {
    return read(
        client
            .get()
            .uri("/public/events/" + slug)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody());
  }

  /** Makes the account of {@code subject} a person of the church; returns that person's id. */
  String linkToAPerson(String subject) {
    var personId =
        read(body(
                alice,
                HttpMethod.POST,
                "/people",
                "{\"firstName\":\"Carol\",\"lastName\":\"Paz\",\"organizationId\":\"%s\"}"
                    .formatted(location)))
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
    return personId;
  }

  static String sharing(String generalAccess) {
    return "{\"generalAccess\":\"%s\"}".formatted(generalAccess);
  }

  String signUp(String name) {
    var subject = "auth0|" + name + "-" + UUID.randomUUID();
    syncCurrentUserCommandHandler.handle(new SyncCurrentUserCommand(subject, email(subject), true));
    return subject;
  }

  static String email(String subject) {
    return subject.replace("auth0|", "").replaceAll("-.*", "") + "@example.org";
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
