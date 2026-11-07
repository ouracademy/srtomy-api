package org.ouracademy.ovejafiel.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
 * Editing events end to end over the real schema (see {@code specs/013-edit-events}): the data of
 * an event, the three options on a series, what is kept when the rule changes, and the email to
 * whoever signed up for an occurrence that moved.
 */
@SpringBootTest(classes = OvejafielApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class EditEventsIntegrationTests {

  static final ObjectMapper JSON = new ObjectMapper();

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  @Autowired JdbcTemplate jdbc;

  RestTestClient client;
  String alice;
  String carol;
  String churchId;

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
  }

  @Test
  void theDataOfAOneOffEventIsEdited() {
    var day = nextDay(5);
    var id = createEvent("Retiro", day, null);

    var response = edit(id, "Retiro de jóvenes", at(day, 16), null, "THIS_EVENT");

    assertThat(response.get("id").asText()).isEqualTo(id);
    assertThat(response.get("notifiedPeople").asInt()).isZero();
    var event = json(alice, "/events/" + id);
    assertThat(event.get("title").asText()).isEqualTo("Retiro de jóvenes");
    assertThat(event.get("address").asText()).isEqualTo("Nueva dirección");
    assertThat(OffsetDateTime.parse(event.get("start").asText()).getHour()).isEqualTo(16);
  }

  @Test
  void theNodeAndTheKindOfAnEventCannotChange() {
    var day = nextDay(5);
    var id = createEvent("Retiro", day, null);

    assertThat(
            status(
                alice,
                HttpMethod.PUT,
                "/events/" + id,
                editJson("Retiro", at(day, 15), null, "THIS_EVENT", churchId, false)))
        .isEqualTo(400);
    assertThat(
            status(
                alice,
                HttpMethod.PUT,
                "/events/" + id,
                editJson("Retiro", at(day, 15), null, "THIS_EVENT", group(), true)))
        .isEqualTo(400);
    assertThat(json(alice, "/events/" + id).get("title").asText()).isEqualTo("Retiro");
  }

  @Test
  void whoIsNotAnEditorCannotEdit() {
    var day = nextDay(5);
    var id = createEvent("Retiro", day, null);

    assertThat(
            status(
                carol,
                HttpMethod.PUT,
                "/events/" + id,
                editJson("x", at(day, 15), null, "THIS_EVENT", group(), false)))
        .isEqualTo(403);
    assertThat(
            status(
                alice,
                HttpMethod.PUT,
                "/events/" + UUID.randomUUID(),
                editJson("x", at(day, 15), null, "THIS_EVENT", group(), false)))
        .isEqualTo(404);
  }

  @Test
  void movingAnEventKeepsWhoSignedUpAndEmailsThem() {
    var day = nextDay(5);
    var id = createEvent("Retiro", day, null);
    var ticket = openRegistration(id);
    signUpFor(id, ticket, email("lucia"));
    signUpFor(id, ticket, email("pedro"));

    var response = edit(id, "Retiro", at(day, 18), null, "THIS_EVENT");

    assertThat(response.get("notifiedPeople").asInt()).isEqualTo(2);
    assertThat(json(alice, "/events/" + id + "/registrations")).hasSize(2);
    assertThat(
            jdbc.queryForList(
                "select recipient from outbox_message where type = 'EVENT_CHANGED' "
                    + "and recipient in (?, ?) order by recipient",
                String.class,
                email("lucia"),
                email("pedro")))
        .containsExactly(email("lucia"), email("pedro"));
  }

  @Test
  void aNewNameOrPlaceEmailsWhoSignedUpButTheSameDataDoesNot() {
    var day = nextDay(5);
    var id = createEvent("Retiro", day, null);
    signUpFor(id, openRegistration(id), email("sin-aviso"));
    var sameData = editJson("Retiro", at(day, 15), null, "THIS_EVENT", group(), false);

    // Same name, place (Av. 1) and time: nothing to tell.
    var quiet =
        read(
            body(
                alice,
                HttpMethod.PUT,
                "/events/" + id,
                sameData.replace("Nueva dirección", "Av. 1")));
    assertThat(quiet.get("notifiedPeople").asInt()).isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from outbox_message where recipient = ?",
                Integer.class,
                email("sin-aviso")))
        .isZero();

    // Another place: they are told.
    var loud = edit(id, "Retiro", at(day, 15), null, "THIS_EVENT");
    assertThat(loud.get("notifiedPeople").asInt()).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from outbox_message where recipient = ? and type = 'EVENT_CHANGED'",
                Integer.class,
                email("sin-aviso")))
        .isEqualTo(1);
  }

  @Test
  void thisEventTakesOneOccurrenceOutOfTheSeries() {
    var start = nextDay(10);
    createEvent("Servicio", start, weekly("\"endType\":\"COUNT\",\"count\":4"));
    var events = eventsBetween(start.minusDays(1), start.plusDays(60));
    var second = events.get(1).get("id").asText();

    edit(second, "Servicio especial", at(start.plusDays(7), 15), null, "THIS_EVENT");

    var after = eventsBetween(start.minusDays(1), start.plusDays(60));
    assertThat(after).hasSize(4);
    assertThat(after.get(1).get("title").asText()).isEqualTo("Servicio especial");
    assertThat(after.get(1).get("recurrence").isNull()).isTrue();
    assertThat(after.get(2).get("title").asText()).isEqualTo("Servicio");
    assertThat(after.get(2).get("recurrence").isNull()).isFalse();
  }

  @Test
  void thisAndTheFollowingSplitsTheSeriesInTwo() {
    var start = nextDay(10);
    createEvent("Servicio", start, weekly("\"endType\":\"COUNT\",\"count\":5"));
    var events = eventsBetween(start.minusDays(1), start.plusDays(60));
    var third = events.get(2).get("id").asText();

    edit(third, "Culto", at(start.plusDays(14), 15), null, "THIS_AND_THE_FOLLOWING_EVENTS");

    var after = eventsBetween(start.minusDays(1), start.plusDays(60));
    assertThat(after)
        .extracting(e -> e.get("title").asText())
        .containsExactly("Servicio", "Servicio", "Culto", "Culto", "Culto");
    assertThat(after.get(2).get("id").asText()).isEqualTo(third);
    var firstSeries = after.get(0).get("recurrence").get("seriesId").asText();
    var secondSeries = after.get(2).get("recurrence").get("seriesId").asText();
    assertThat(firstSeries).isNotEqualTo(secondSeries);
    assertThat(after.get(1).get("recurrence").get("seriesId").asText()).isEqualTo(firstSeries);
    assertThat(after.get(4).get("recurrence").get("seriesId").asText()).isEqualTo(secondSeries);
    // 5 in total: the first series keeps 2 and the second the 3 that were left.
    assertThat(after.get(2).get("recurrence").get("count").asInt()).isEqualTo(3);
  }

  @Test
  void allEventsInPlaceChangesTheTimeAndKeepsEveryoneSignedUp() {
    var start = nextDay(10);
    createEvent("Servicio", start, weekly("\"endType\":\"COUNT\",\"count\":3"));
    var events = eventsBetween(start.minusDays(1), start.plusDays(60));
    var ids = events.findValuesAsText("id");
    var second = events.get(1).get("id").asText();
    signUpFor(second, openRegistration(second), email("ana"));

    var response = edit(ids.get(0), "Servicio", at(start, 17), null, "ALL_EVENTS");

    assertThat(response.get("notifiedPeople").asInt()).isEqualTo(1);
    var after = eventsBetween(start.minusDays(1), start.plusDays(60));
    assertThat(after.findValuesAsText("id")).containsExactlyElementsOf(ids);
    assertThat(after)
        .allSatisfy(
            e -> assertThat(OffsetDateTime.parse(e.get("start").asText()).getHour()).isEqualTo(17));
    assertThat(json(alice, "/events/" + second + "/registrations")).hasSize(1);
  }

  @Test
  void changingTheRuleKeepsTheOccurrencesWithPeopleAndReplacesTheRest() {
    var start = nextDay(10);
    createEvent("Servicio", start, weekly("\"endType\":\"COUNT\",\"count\":4"));
    var events = eventsBetween(start.minusDays(1), start.plusDays(60));
    var first = events.get(0).get("id").asText();
    var third = events.get(2).get("id").asText();
    signUpFor(third, openRegistration(third), email("ana"));

    var response =
        edit(
            first,
            "Servicio",
            at(start, 15),
            "{\"frequency\":\"DAILY\",\"endType\":\"COUNT\",\"count\":3}",
            "ALL_EVENTS");

    assertThat(response.get("keptEvents").asInt()).isEqualTo(1);
    var after = eventsBetween(start.minusDays(1), start.plusDays(60));
    // 3 daily from the first day, and the one with a sign-up that was left alone, out of the
    // series.
    assertThat(after).hasSize(4);
    assertThat(after.findValuesAsText("id")).contains(first, third);
    JsonNode kept = null;
    for (var e : after) {
      if (e.get("id").asText().equals(third)) {
        kept = e;
      }
    }
    assertThat(kept.get("recurrence").isNull()).isTrue();
    assertThat(kept.get("title").asText()).isEqualTo("Servicio");
    assertThat(json(alice, "/events/" + third + "/registrations")).hasSize(1);
    assertThat(after.findValuesAsText("title").stream().filter("Servicio"::equals).count())
        .isEqualTo(4);
  }

  @Test
  void aOneOffEventCanStartRepeating() {
    var day = nextDay(5);
    var id = createEvent("Retiro", day, null);

    edit(id, "Retiro", at(day, 15), weekly("\"endType\":\"COUNT\",\"count\":3"), "THIS_EVENT");

    var events = eventsBetween(day.minusDays(1), day.plusDays(30));
    assertThat(events).hasSize(3);
    assertThat(events.get(0).get("id").asText()).isEqualTo(id);
    assertThat(events.get(0).get("recurrence").get("frequency").asText()).isEqualTo("WEEKLY");
  }

  // ---- helpers

  final String runId = UUID.randomUUID().toString().substring(0, 8);

  /** Distinct per test run: the database is not emptied between runs. */
  String email(String name) {
    return name + "-" + runId + "@example.org";
  }

  OffsetDateTime at(LocalDate day, int hour) {
    return day.atTime(hour, 0).atOffset(ZoneOffset.UTC);
  }

  String editJson(
      String name,
      OffsetDateTime start,
      String recurrence,
      String option,
      String organizationId,
      boolean course) {
    return ("{\"name\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\",\"organizationId\":\"%s\","
            + "\"address\":\"Nueva dirección\",\"isACourse\":%s,\"option\":\"%s\","
            + "\"timeZone\":\"America/Lima\"%s}")
        .formatted(
            name,
            start,
            start.plusHours(2),
            organizationId,
            course,
            option,
            recurrence == null ? "" : ",\"recurrence\":" + recurrence);
  }

  JsonNode edit(String id, String name, OffsetDateTime start, String recurrence, String option) {
    return read(
        body(
            alice,
            HttpMethod.PUT,
            "/events/" + id,
            editJson(name, start, recurrence, option, group(), false)));
  }

  String openRegistration(String eventId) {
    var ticket =
        read(body(
                alice,
                HttpMethod.PUT,
                "/events/" + eventId + "/registration",
                "{\"requiresAccount\":false,\"tickets\":[{\"name\":\"General\",\"priceCents\":0,"
                    + "\"currency\":\"PEN\",\"quantity\":50}]}"))
            .get("tickets")
            .get(0)
            .get("id")
            .asText();
    body(alice, HttpMethod.PUT, "/events/" + eventId + "/registration/open", "{\"open\":true}");
    body(
        alice,
        HttpMethod.PUT,
        "/events/" + eventId + "/sharing",
        "{\"generalAccess\":\"ANYONE_WITH_LINK\"}");
    return ticket;
  }

  void signUpFor(String eventId, String ticketId, String email) {
    var slug = json(alice, "/events/" + eventId + "/sharing").get("slug").asText();
    client
        .post()
        .uri("/public/events/" + slug + "/registrations")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            "{\"ticketTypeId\":\"%s\",\"firstName\":\"Lu\",\"lastName\":\"Mora\",\"email\":\"%s\"}"
                .formatted(ticketId, email))
        .exchange()
        .expectStatus()
        .is2xxSuccessful();
  }

  // ---- helpers

  String groupId;

  LocalDate nextDay(int days) {
    return LocalDate.now(ZoneOffset.UTC).plusDays(days);
  }

  static String weekly(String end) {
    return "{\"frequency\":\"WEEKLY\",\"interval\":1,\"daysOfWeek\":[],%s}".formatted(end);
  }

  String group() {
    if (groupId == null) {
      var location =
          json(alice, "/organizations/" + churchId + "/childs").get(0).get("id").asText();
      groupId =
          read(body(
                  alice,
                  HttpMethod.POST,
                  "/organizations/locations/childs",
                  "{\"name\":\"Jóvenes\",\"type\":\"GROUP\",\"parentId\":\"%s\"}"
                      .formatted(location)))
              .get("id")
              .asText();
    }
    return groupId;
  }

  String eventBody(String name, LocalDate day, String recurrence) {
    var start = day.atTime(15, 0).atOffset(ZoneOffset.UTC);
    return ("{\"name\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\",\"organizationId\":\"%s\","
            + "\"address\":\"Av. 1\",\"timeZone\":\"America/Lima\"%s}")
        .formatted(
            name,
            start,
            start.plusHours(2),
            group(),
            recurrence == null ? "" : ",\"recurrence\":" + recurrence);
  }

  String createEvent(String name, LocalDate day, String recurrence) {
    return read(body(alice, HttpMethod.POST, "/events", eventBody(name, day, recurrence))).asText();
  }

  JsonNode eventsBetween(LocalDate from, LocalDate to) {
    var found =
        json(
            alice,
            "/events?organizationId=%s&start=%sT00:00:00&end=%sT00:00:00"
                .formatted(group(), from, to));
    // The API does not promise an order, and an update moves a row in the table.
    var sorted = new java.util.ArrayList<JsonNode>();
    found.forEach(sorted::add);
    sorted.sort(java.util.Comparator.comparing(e -> e.get("start").asText()));
    return JSON.createArrayNode().addAll(sorted);
  }

  String signUp(String name) {
    var subject = "auth0|" + name + "-" + UUID.randomUUID();
    syncCurrentUserCommandHandler.handle(
        new SyncCurrentUserCommand(subject, subject.replace("auth0|", "") + "@example.org", true));
    return subject;
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
