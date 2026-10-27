package org.ouracademy.ovejafiel.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Repeating events and the calendar feed end to end over the real schema: the series are created
 * with their occurrences, deleted by option, and the events of the user show up in a feed that
 * calendar apps read with only the token of the address.
 */
@SpringBootTest(classes = OvejafielApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class RecurringEventsIntegrationTests {

  static final ObjectMapper JSON = new ObjectMapper();

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;

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
  void aWeeklySeriesIsCreatedWithItsOccurrencesAndTheRecurrence() {
    var start = nextDay(10);
    var firstId = createEvent("Servicio", start, weekly("\"endType\":\"COUNT\",\"count\":5"));

    var events = eventsBetween(start.minusDays(1), start.plusDays(60));

    assertThat(events).hasSize(5);
    assertThat(events.get(0).get("id").asText()).isEqualTo(firstId);
    var recurrence = events.get(0).get("recurrence");
    assertThat(recurrence.get("frequency").asText()).isEqualTo("WEEKLY");
    assertThat(recurrence.get("endType").asText()).isEqualTo("COUNT");
    assertThat(recurrence.get("count").asInt()).isEqualTo(5);
    assertThat(recurrence.get("timeZone").asText()).isEqualTo("America/Lima");
    assertThat(recurrence.get("seriesId").asText())
        .isEqualTo(events.get(4).get("recurrence").get("seriesId").asText());
  }

  @Test
  void aSeriesThatNeverEndsHasAYearOfOccurrences() {
    var start = nextDay(1);
    createEvent("Oración", start, "{\"frequency\":\"DAILY\",\"endType\":\"NEVER\"}");

    var events = eventsBetween(start.minusDays(1), start.plusDays(400));

    assertThat(events.size()).isBetween(365, 367);
  }

  @Test
  void aOneOffEventHasNoRecurrence() {
    var start = nextDay(3);
    createEvent("Retiro", start, null);

    var events = eventsBetween(start.minusDays(1), start.plusDays(1));

    assertThat(events).hasSize(1);
    assertThat(events.get(0).get("recurrence").isNull()).isTrue();
  }

  @Test
  void aRepeatWithoutTheDateItEndsOnIsRejected() {
    var start = nextDay(3);

    assertThat(
            status(
                alice,
                HttpMethod.POST,
                "/events",
                eventBody("Servicio", start, "{\"frequency\":\"DAILY\",\"endType\":\"UNTIL\"}")))
        .isEqualTo(400);
  }

  @Test
  void deletingThisAndTheFollowingEventsKeepsTheEarlierOnesAndEndsTheSeries() {
    var start = nextDay(10);
    createEvent("Servicio", start, weekly("\"endType\":\"COUNT\",\"count\":5"));
    var events = eventsBetween(start.minusDays(1), start.plusDays(60));

    body(
        alice,
        HttpMethod.DELETE,
        "/events/" + events.get(2).get("id").asText(),
        "{\"option\":\"THIS_AND_THE_FOLLOWING_EVENTS\"}");

    assertThat(eventsBetween(start.minusDays(1), start.plusDays(60))).hasSize(2);
  }

  @Test
  void deletingAllEventsRemovesTheWholeSeries() {
    var start = nextDay(10);
    createEvent("Servicio", start, weekly("\"endType\":\"COUNT\",\"count\":5"));
    var events = eventsBetween(start.minusDays(1), start.plusDays(60));

    body(
        alice,
        HttpMethod.DELETE,
        "/events/" + events.get(1).get("id").asText(),
        "{\"option\":\"ALL_EVENTS\"}");

    assertThat(eventsBetween(start.minusDays(1), start.plusDays(60))).isEmpty();
  }

  @Test
  void theCalendarFeedListsTheEventsOfTheUserAndNeedsNoSession() {
    var start = nextDay(5);
    createEvent("Servicio", start, weekly("\"endType\":\"COUNT\",\"count\":3"));
    createEvent("Retiro", start.plusDays(1), null);

    var path = feedPath(alice);
    var response =
        client
            .get()
            .uri(path.substring("/v1".length()))
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith("text/calendar")
            .expectBody(String.class)
            .returnResult();
    var calendar = response.getResponseBody();

    assertThat(calendar).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n");
    assertThat(calendar.split("BEGIN:VEVENT", -1)).hasSize(5); // 3 + 1 events
    assertThat(calendar).contains("SUMMARY:Servicio · Jóvenes");
    assertThat(calendar).contains("SUMMARY:Retiro · Jóvenes");
    assertThat(calendar).contains("LOCATION:Av. 1");
  }

  @Test
  void aUserWithoutAccessGetsAnEmptyCalendar() {
    createEvent("Servicio", nextDay(5), null);

    var calendar =
        client
            .get()
            .uri(feedPath(carol).substring("/v1".length()))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(calendar).doesNotContain("BEGIN:VEVENT");
  }

  @Test
  void theFeedAddressIsTheSameUntilItIsRenewed() {
    var first = feedPath(alice);
    assertThat(feedPath(alice)).isEqualTo(first);

    var renewed =
        read(body(alice, HttpMethod.POST, "/me/calendar-feed/renew", null)).get("path").asText();

    assertThat(renewed).isNotEqualTo(first).startsWith("/v1/public/calendar/").endsWith(".ics");
    assertThat(feedPath(alice)).isEqualTo(renewed);
    client.get().uri(first.substring("/v1".length())).exchange().expectStatus().isNotFound();
    client.get().uri(renewed.substring("/v1".length())).exchange().expectStatus().isOk();
  }

  @Test
  void anUnknownTokenIsNotFound() {
    client.get().uri("/public/calendar/nope.ics").exchange().expectStatus().isNotFound();
  }

  @Test
  void theFeedAddressNeedsASession() {
    client.get().uri("/me/calendar-feed").exchange().expectStatus().isUnauthorized();
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
    return json(
        alice,
        "/events?organizationId=%s&start=%sT00:00:00&end=%sT00:00:00".formatted(group(), from, to));
  }

  String feedPath(String subject) {
    return json(subject, "/me/calendar-feed").get("path").asText();
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
