package org.ouracademy.ovejafiel.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
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
 * The daily reminder over the real schema: someone signed up for an event of tomorrow gets one
 * WhatsApp reminder, only once, and not if their church has no WhatsApp or they asked to stop.
 */
@SpringBootTest(classes = OvejafielApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class WhatsAppReminderIntegrationTests {

  static final ObjectMapper JSON = new ObjectMapper();
  static final String REMIND = "/v1/internal/events/whatsapp-reminders";

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  @Autowired JdbcTemplate jdbc;

  RestTestClient client;
  String alice;
  String churchId;
  String group;
  String slug;
  String ticket;
  String phone;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    alice = signUp("alice");
    churchId =
        read(call(
                HttpMethod.POST,
                "/v1/organizations",
                """
                    {"name":"Iglesia","location":{"name":"Sede"},
                     "positions":[{"name":"Pastor","roleIds":["%s"]}],
                     "creator":{"firstName":"Ana","lastName":"Diaz","positionName":"Pastor"}}
                    """
                    .formatted(PredefinedRoles.ADMIN.value())))
            .get("id")
            .asText();
    var location =
        read(call(HttpMethod.GET, "/v1/organizations/" + churchId + "/childs", null))
            .get(0)
            .get("id")
            .asText();
    group =
        read(call(
                HttpMethod.POST,
                "/v1/organizations/locations/childs",
                "{\"name\":\"Jóvenes\",\"type\":\"GROUP\",\"parentId\":\"%s\"}"
                    .formatted(location)))
            .get("id")
            .asText();
    phone =
        "519" + java.util.concurrent.ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
  }

  /** An event that starts in {@code hours} hours with one person signed up with a phone. */
  void eventIn(long hours) {
    var start = OffsetDateTime.now().plusHours(hours);
    var event =
        read(call(
                HttpMethod.POST,
                "/v1/events",
                ("{\"name\":\"Culto de jóvenes\",\"startTime\":\"%s\",\"endTime\":\"%s\","
                        + "\"address\":\"Av. Perú 123\",\"organizationId\":\"%s\","
                        + "\"isACourse\":false}")
                    .formatted(start, start.plusHours(2), group)))
            .asText();
    slug =
        read(call(HttpMethod.GET, "/v1/events/" + event + "/sharing", null)).get("slug").asText();
    call(
        HttpMethod.PUT,
        "/v1/events/" + event + "/sharing",
        "{\"generalAccess\":\"ANYONE_WITH_LINK\"}");
    call(
        HttpMethod.PUT,
        "/v1/events/" + event + "/registration",
        "{\"requiresAccount\":false,"
            + "\"tickets\":[{\"name\":\"General\",\"priceCents\":0,\"quantity\":5}]}");
    ticket =
        read(call(HttpMethod.GET, "/v1/events/" + event + "/registration", null))
            .get("tickets")
            .get(0)
            .get("id")
            .asText();
    var signedUp =
        client
            .post()
            .uri("/v1/public/events/" + slug + "/registrations")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                ("{\"ticketTypeId\":\"%s\",\"firstName\":\"Lucía\",\"lastName\":\"Prueba\","
                        + "\"email\":\"lucia-%s@example.org\",\"phoneNumber\":\"%s\"}")
                    .formatted(ticket, UUID.randomUUID(), phone))
            .exchange()
            .returnResult(String.class);
    assertThat(signedUp.getStatus().is2xxSuccessful()).isTrue();
  }

  void connectWhatsApp() {
    jdbc.update(
        "insert into whatsapp_connection(organization_id, kapso_customer_id, mode, status,"
            + " phone_number_id, display_number, connected_at, create_date, last_modified_date)"
            + " values (?::uuid, ?, 'DEDICATED', 'CONNECTED', ?, '+51 1', now(), now(), now())",
        churchId,
        "cus-" + UUID.randomUUID(),
        "pn-" + UUID.randomUUID());
  }

  JsonNode remind(String token) {
    var answer =
        client
            .post()
            .uri(REMIND)
            .headers(h -> h.setBearerAuth(token))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult();
    return read(answer.getResponseBody());
  }

  int reminders() {
    return jdbc.queryForObject(
        "select count(*) from outbox_message where type = 'EVENT_REMINDER' and recipient = ?",
        Integer.class,
        phone);
  }

  @Test
  void aSignedUpPersonGetsOneReminderAndASecondRunDoesNotRepeatIt() {
    connectWhatsApp();
    eventIn(20);

    var first = remind(TestSecurityConfig.schedulerToken(jwtEncoder));
    var second = remind(TestSecurityConfig.schedulerToken(jwtEncoder));

    assertThat(first.get("queued").asInt()).isPositive();
    assertThat(reminders()).isEqualTo(1);
    assertThat(second.get("queued").asInt()).isZero();
    assertThat(reminders()).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select channel from outbox_message where type = 'EVENT_REMINDER'"
                    + " and recipient = ?",
                String.class,
                phone))
        .isEqualTo("WHATSAPP");
  }

  @Test
  void anEventOutsideTheWindowIsNotRemindedYet() {
    connectWhatsApp();
    eventIn(60);

    remind(TestSecurityConfig.schedulerToken(jwtEncoder));

    assertThat(reminders()).isZero();
  }

  @Test
  void aChurchWithoutWhatsappSendsNothing() {
    eventIn(20);

    remind(TestSecurityConfig.schedulerToken(jwtEncoder));

    assertThat(reminders()).isZero();
  }

  @Test
  void aPersonOfTheChurchWhoAskedToStopIsNotReminded() {
    connectWhatsApp();
    var person =
        read(call(
                HttpMethod.POST,
                "/v1/people",
                ("{\"firstName\":\"Lucía\",\"lastName\":\"Prueba\",\"phoneNumber\":\"%s\","
                        + "\"organizationId\":\"%s\",\"whatsappConsent\":false}")
                    .formatted(phone, group)))
            .asText();
    assertThat(person).isNotBlank();
    eventIn(20);

    remind(TestSecurityConfig.schedulerToken(jwtEncoder));

    assertThat(reminders()).isZero();
  }

  @Test
  void onlyTheSchedulerCanStartTheReminders() {
    assertThat(statusWith(null)).isEqualTo(401);
    assertThat(statusWith(TestSecurityConfig.validToken(jwtEncoder, alice))).isEqualTo(401);
    assertThat(statusWith(TestSecurityConfig.agentToken(jwtEncoder))).isEqualTo(403);
    assertThat(statusWith(TestSecurityConfig.schedulerToken(jwtEncoder))).isEqualTo(200);
  }

  // --- helpers

  int statusWith(String token) {
    return client
        .post()
        .uri(REMIND)
        .headers(
            h -> {
              if (token != null) {
                h.setBearerAuth(token);
              }
            })
        .exchange()
        .returnResult(String.class)
        .getStatus()
        .value();
  }

  String signUp(String name) {
    var subject = "auth0|" + name + "-" + UUID.randomUUID();
    syncCurrentUserCommandHandler.handle(
        new SyncCurrentUserCommand(subject, name + "@example.org", true));
    return subject;
  }

  String call(HttpMethod method, String uri, String body) {
    var spec =
        client
            .method(method)
            .uri(uri)
            .headers(h -> h.setBearerAuth(TestSecurityConfig.validToken(jwtEncoder, alice)));
    var exchanged =
        body == null
            ? spec.exchange()
            : spec.contentType(MediaType.APPLICATION_JSON).body(body).exchange();
    var result = exchanged.returnResult(String.class);
    assertThat(result.getStatus().is2xxSuccessful()).as("%s %s", method, uri).isTrue();
    return result.getResponseBody();
  }

  static JsonNode read(String body) {
    try {
      return JSON.readTree(body);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
