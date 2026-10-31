package org.ouracademy.ovejafiel.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.DelegatedTokens;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.ouracademy.ovejafiel.user.command.link_user_to_person.LinkUserToPersonCommand;
import org.ouracademy.ovejafiel.user.command.link_user_to_person.LinkUserToPersonCommandHandler;
import org.ouracademy.ovejafiel.user.command.sync_current_user.SyncCurrentUserCommand;
import org.ouracademy.ovejafiel.user.command.sync_current_user.SyncCurrentUserCommandHandler;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Who writes on WhatsApp, over the real schema and the real security chains: the agent asks who
 * wrote, gets a token to act as that person at the Member level (never the rights of a position),
 * and every other way in is closed.
 */
@SpringBootTest(classes = OvejafielApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class WhatsAppIdentityIntegrationTests {

  static final ObjectMapper JSON = new ObjectMapper();
  static final String CHURCH_NUMBER = "pn-it-1";
  static RSAKey signingKey;

  @DynamicPropertySource
  static void delegationKey(DynamicPropertyRegistry registry) throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    var pair = generator.generateKeyPair();
    signingKey =
        new RSAKey.Builder((RSAPublicKey) pair.getPublic())
            .privateKey((RSAPrivateKey) pair.getPrivate())
            .build();
    var pem =
        "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(pair.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----";
    registry.add("delegation.private-key", () -> pem);
  }

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;
  @Autowired LinkUserToPersonCommandHandler linkUserToPersonCommandHandler;
  @Autowired JdbcTemplate jdbc;

  RestTestClient client;
  String alice;
  String churchId;
  String group;
  String phoneNumberId;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    phoneNumberId = CHURCH_NUMBER + "-" + UUID.randomUUID();
    alice = signUp("alice");
    var created =
        read(
            call(
                alice,
                HttpMethod.POST,
                "/v1/organizations",
                """
                {"name":"Iglesia","location":{"name":"Sede"},
                 "positions":[{"name":"Pastor","roleIds":["%s"]}],
                 "creator":{"firstName":"Ana","lastName":"Diaz","positionName":"Pastor"}}
                """
                    .formatted(PredefinedRoles.ADMIN.value())));
    churchId = created.get("id").asText();
    var location =
        read(call(alice, HttpMethod.GET, "/v1/organizations/" + churchId + "/childs", null))
            .get(0)
            .get("id")
            .asText();
    group =
        read(call(
                alice,
                HttpMethod.POST,
                "/v1/organizations/locations/childs",
                "{\"name\":\"Grupo\",\"type\":\"GROUP\",\"parentId\":\"%s\"}".formatted(location)))
            .get("id")
            .asText();
    jdbc.update(
        "insert into whatsapp_connection(organization_id, kapso_customer_id, mode, status,"
            + " phone_number_id, display_number, connected_at, create_date, last_modified_date)"
            + " values (?::uuid, ?, 'DEDICATED', 'CONNECTED', ?, '+51 1', now(), now(), now())",
        churchId,
        "cus-" + UUID.randomUUID(),
        phoneNumberId);
  }

  // --- who may ask

  @Test
  void onlyTheAgentsServiceAccountCanAsk() {
    var body = "{\"phoneNumberId\":\"%s\",\"from\":\"51999111222\"}".formatted(phoneNumberId);

    assertThat(identityStatus(null, body)).isEqualTo(401);
    assertThat(identityStatus(TestSecurityConfig.validToken(jwtEncoder), body)).isEqualTo(401);
    assertThat(identityStatus(TestSecurityConfig.schedulerToken(jwtEncoder), body)).isEqualTo(403);
    assertThat(identityStatus(TestSecurityConfig.agentToken(jwtEncoder), body)).isEqualTo(200);
  }

  @Test
  void theAgentCannotCallWhatTheSchedulerCalls() {
    var status =
        client
            .post()
            .uri("/v1/internal/outbox/dispatch-pending")
            .headers(h -> h.setBearerAuth(TestSecurityConfig.agentToken(jwtEncoder)))
            .exchange()
            .returnResult(String.class)
            .getStatus()
            .value();

    assertThat(status).isEqualTo(403);
  }

  // --- who writes

  @Test
  void aMemberWithoutAccountActsAtMemberLevelAndNothingMore() {
    createPerson("Lucía", "999 111 222", group, null);

    var identity = identity("51999111222");

    assertThat(identity.get("status").asText()).isEqualTo("LINKED");
    assertThat(identity.get("name").asText()).isEqualTo("Lucía Prueba");
    var token = identity.get("token").asText();
    // Member level: can read the group she belongs to ...
    assertThat(status(token, HttpMethod.GET, "/v1/organizations/" + group)).isEqualTo(200);
    // ... but not the people, nor write anything.
    assertThat(status(token, HttpMethod.GET, "/v1/people?organizationId=" + churchId))
        .isEqualTo(403);
    assertThat(
            status(
                token,
                HttpMethod.POST,
                "/v1/people",
                "{\"firstName\":\"X\",\"lastName\":\"Y\",\"organizationId\":\"%s\"}"
                    .formatted(churchId)))
        .isEqualTo(403);
  }

  @Test
  void aPhoneTypedOnTheRecordOfAPastorNeverGivesThePastorsRights() {
    var pastorType =
        jdbc.queryForObject(
            "select id::text from accountability_type where party_id = ?::uuid"
                + " and name = 'Pastor' and category = 'POSITION'",
            String.class,
            churchId);
    var carlos = createPerson("Carlos", "999 333 444", churchId, pastorType);

    var identity = identity("51999333444");

    assertThat(identity.get("status").asText()).isEqualTo("LINKED");
    var token = identity.get("token").asText();
    // The control: a real account holding the same kind of position does get its access ...
    assertThat(
            jdbc.queryForObject(
                "select count(*) from user_access ua join app_user u on u.id = ua.user_id"
                    + " where u.auth0_sub = ? and ua.source = 'POSITION'",
                Integer.class,
                alice))
        .isPositive();
    // ... while his position is an Administrator, yet through his phone alone he is a Member.
    assertThat(status(token, HttpMethod.GET, "/v1/people?organizationId=" + churchId))
        .isEqualTo(403);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from user_access ua join app_user u on u.id = ua.user_id"
                    + " where u.auth0_sub = ? and ua.source = 'POSITION'",
                Integer.class,
                "whatsapp:" + carlos))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from user_access ua join app_user u on u.id = ua.user_id"
                    + " where u.auth0_sub = ? and ua.source = 'MEMBER'",
                Integer.class,
                "whatsapp:" + carlos))
        .isPositive();
  }

  @Test
  void aPersonWithARealAccountIsNotLinkedByPhoneAlone() {
    jdbc.update(
        "update party set phone_number = '999 555 666' where id in"
            + " (select person_id from user_person where church_id = ?::uuid)",
        churchId);

    var identity = identity("51999555666");

    assertThat(identity.get("status").asText()).isEqualTo("NEEDS_VERIFICATION");
    assertThat(identity.get("token").isNull()).isTrue();
  }

  @Test
  void unknownAndAmbiguousPhonesAndOtherNumbersGetNoToken() {
    createPerson("Marta", "999 777 888", group, null);
    createPerson("Mario", "999 777 888", group, null);

    assertThat(identity("51999000000").get("status").asText()).isEqualTo("UNKNOWN");
    assertThat(identity("51999777888").get("status").asText()).isEqualTo("AMBIGUOUS");
    var other =
        read(identityBody(TestSecurityConfig.agentToken(jwtEncoder), "pn-nobody", "51999777888"));
    assertThat(other.get("status").asText()).isEqualTo("CHURCH_NOT_CONNECTED");
    assertThat(other.get("token").isNull()).isTrue();
  }

  @Test
  void anyoneEvenIfUnknownCanAskAboutTheChurchThroughItsPublicPage() {
    var identity = identity("51999000000");

    assertThat(identity.get("status").asText()).isEqualTo("UNKNOWN");
    assertThat(identity.get("token").isNull()).isTrue();
    assertThat(identity.get("churchName").asText()).isEqualTo("Iglesia");
    var slug = identity.get("churchSlug").asText();
    // No token at all: what the church publishes is for everyone ...
    var page = client.get().uri("/v1/public/organizations/" + slug).exchange();
    assertThat(page.returnResult(String.class).getStatus().value()).isEqualTo(200);
    // ... and nothing else is: people, groups and the like need a person.
    assertThat(
            client
                .get()
                .uri("/v1/people?organizationId=" + churchId)
                .exchange()
                .returnResult(String.class)
                .getStatus()
                .value())
        .isEqualTo(401);
  }

  @Test
  void aDisconnectedChurchRecognizesNobody() {
    createPerson("Lucía", "999 111 222", group, null);
    jdbc.update(
        "update whatsapp_connection set status = 'DISCONNECTED' where organization_id = ?::uuid",
        churchId);

    assertThat(identity("51999111222").get("status").asText()).isEqualTo("CHURCH_NOT_CONNECTED");
  }

  // --- the token

  @Test
  void aTokenForgedWithAnotherKeyOrWronglyIssuedIsRefused() throws Exception {
    var other = new RSAKey.Builder(signingKey.toRSAPublicKey());
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    var pair = generator.generateKeyPair();
    var forged =
        DelegatedTokens.of(
                new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyIDFromThumbprint()
                    .build())
            .issue(alice)
            .token();

    assertThat(other).isNotNull();
    assertThat(status(forged, HttpMethod.GET, "/v1/organizations/" + churchId)).isEqualTo(401);
  }

  // --- stopping

  @Test
  void stopTurnsTheNoticesOffForThatPhone() {
    var lucia = createPerson("Lucía", "999 111 222", group, null);

    var status =
        client
            .post()
            .uri("/v1/internal/whatsapp/opt-out")
            .headers(h -> h.setBearerAuth(TestSecurityConfig.agentToken(jwtEncoder)))
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"phoneNumberId\":\"%s\",\"from\":\"51999111222\"}".formatted(phoneNumberId))
            .exchange()
            .returnResult(String.class);

    assertThat(status.getStatus().value()).isEqualTo(200);
    assertThat(
            jdbc.queryForObject(
                "select whatsapp_consent from person where id = ?::uuid", Boolean.class, lucia))
        .isFalse();
  }

  // --- a real account later

  @Test
  void aRealAccountLinkedLaterReplacesTheUserMadeForWhatsApp() {
    var lucia = createPerson("Lucía", "999 111 222", group, null);
    assertThat(identity("51999111222").get("status").asText()).isEqualTo("LINKED");
    var shadows =
        jdbc.queryForObject(
            "select count(*) from app_user where auth0_sub = ?",
            Integer.class,
            "whatsapp:" + lucia);
    assertThat(shadows).isEqualTo(1);

    var bobSubject = signUp("bob");
    var bobId =
        jdbc.queryForObject(
            "select id::text from app_user where auth0_sub = ?", String.class, bobSubject);
    linkUserToPersonCommandHandler.handle(
        new LinkUserToPersonCommand(
            new UserId(UUID.fromString(bobId)),
            new PartyId(UUID.fromString(lucia)),
            new PartyId(UUID.fromString(churchId))));

    assertThat(
            jdbc.queryForObject(
                "select count(*) from app_user where auth0_sub = ?",
                Integer.class,
                "whatsapp:" + lucia))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select user_id::text from user_person where person_id = ?::uuid",
                String.class,
                lucia))
        .isEqualTo(bobId);
    // From now on she has an account: the phone alone is not enough any more.
    assertThat(identity("51999111222").get("status").asText()).isEqualTo("NEEDS_VERIFICATION");
  }

  // --- linking a number to an account with a code

  @Test
  void aSignedInPersonProvesTheirNumberAndThenActsWithAllTheirRights() {
    var phone0 = freshPhone();
    var phone1 = freshPhone();
    var code = linkCode(alice);

    assertThat(linkStatus(phone0, code.toLowerCase().replace("-", " "))).isEqualTo("LINKED");

    var identity = identity(phone0);
    assertThat(identity.get("status").asText()).isEqualTo("LINKED");
    assertThat(identity.get("level").asText()).isEqualTo("ACCOUNT");
    assertThat(identity.get("name").asText()).isEqualTo("Ana Diaz");
    // Her account is an Administrator: through the proven number she can do what it can.
    var token = identity.get("token").asText();
    assertThat(status(token, HttpMethod.GET, "/v1/people?organizationId=" + churchId))
        .isEqualTo(200);
    // The code was used up.
    assertThat(linkStatus(phone1, code)).isEqualTo("INVALID_CODE");
    // The portal shows it linked, with the number hidden.
    var link =
        read(call(alice, HttpMethod.GET, "/v1/organizations/" + churchId + "/whatsapp/link", null));
    assertThat(link.get("linked").asBoolean()).isTrue();
    assertThat(link.get("maskedPhone").asText())
        .endsWith(phone0.substring(phone0.length() - 4))
        .doesNotContain(phone0.substring(0, 8));
  }

  @Test
  void aWrongCodeLinksNothingAndTooManyBlockEvenTheRightOne() {
    var phone2 = freshPhone();
    var phone3 = freshPhone();
    var code = linkCode(alice);

    for (var i = 0; i < 5; i++) {
      assertThat(linkStatus(phone2, "ZZZZ-000" + i)).isEqualTo("INVALID_CODE");
    }
    assertThat(linkStatus(phone2, code)).isEqualTo("RATE_LIMITED");
    assertThat(identity(phone2).get("level").isNull()).isTrue();
    // Another number is not blocked by what the first one did.
    assertThat(linkStatus(phone3, code)).isEqualTo("LINKED");
  }

  @Test
  void anExpiredCodeIsRefused() {
    var phone4 = freshPhone();
    var code = linkCode(alice);
    jdbc.update("update whatsapp_link_code set expires_at = now() - interval '1 minute'");

    assertThat(linkStatus(phone4, code)).isEqualTo("INVALID_CODE");
  }

  @Test
  void aCodeOfAnotherChurchDoesNotWorkHere() {
    var code = linkCode(alice);
    var otherNumber = "pn-other-" + UUID.randomUUID();
    var otherChurch =
        read(call(
                alice,
                HttpMethod.POST,
                "/v1/organizations",
                "{\"name\":\"Otra\",\"location\":{\"name\":\"Sede\"},\"positions\":[],"
                    + "\"creator\":{\"firstName\":\"Ana\",\"lastName\":\"Otra\"}}"))
            .get("id")
            .asText();
    jdbc.update(
        "insert into whatsapp_connection(organization_id, kapso_customer_id, mode, status,"
            + " phone_number_id, display_number, connected_at, create_date, last_modified_date)"
            + " values (?::uuid, ?, 'DEDICATED', 'CONNECTED', ?, '+51 2', now(), now(), now())",
        otherChurch,
        "cus-" + UUID.randomUUID(),
        otherNumber);

    var body = "{\"phoneNumberId\":\"%s\",\"from\":\"%s\",\"code\":\"%s\"}";
    var answer =
        client
            .post()
            .uri("/v1/internal/whatsapp/link")
            .headers(h -> h.setBearerAuth(TestSecurityConfig.agentToken(jwtEncoder)))
            .contentType(MediaType.APPLICATION_JSON)
            .body(body.formatted(otherNumber, freshPhone(), code))
            .exchange()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

    assertThat(read(answer).get("status").asText()).isEqualTo("INVALID_CODE");
  }

  @Test
  void unlinkingTakesTheAccountBackToAGuess() {
    var phone5 = freshPhone();
    linkStatus(phone5, linkCode(alice));
    assertThat(identity(phone5).get("level").asText()).isEqualTo("ACCOUNT");

    call(alice, HttpMethod.DELETE, "/v1/organizations/" + churchId + "/whatsapp/link", null);

    assertThat(identity(phone5).get("status").asText()).isEqualTo("UNKNOWN");
  }

  @Test
  void onlyAPersonOfTheChurchGetsACodeAndOnlyWhenSignedIn() {
    var bob = signUp("bob");

    assertThat(
            status(
                TestSecurityConfig.validToken(jwtEncoder, bob),
                HttpMethod.POST,
                "/v1/organizations/" + churchId + "/whatsapp/link-code"))
        .isEqualTo(400);
    assertThat(
            client
                .post()
                .uri("/v1/organizations/" + churchId + "/whatsapp/link-code")
                .exchange()
                .returnResult(String.class)
                .getStatus()
                .value())
        .isEqualTo(401);
  }

  @Test
  void theCodeIsNotKeptAndTheEndpointOfTheAgentIsClosedToEveryoneElse() {
    var code = linkCode(alice);

    assertThat(
            jdbc.queryForObject(
                "select count(*) from whatsapp_link_code where code_hash like '%' || ? || '%'",
                Integer.class, code.replace("-", "")))
        .isZero();
    var body = "{\"phoneNumberId\":\"%s\",\"from\":\"%s\",\"code\":\"%s\"}";
    var status =
        client
            .post()
            .uri("/v1/internal/whatsapp/link")
            .headers(h -> h.setBearerAuth(TestSecurityConfig.validToken(jwtEncoder, alice)))
            .contentType(MediaType.APPLICATION_JSON)
            .body(body.formatted(phoneNumberId, freshPhone(), code))
            .exchange()
            .returnResult(String.class)
            .getStatus()
            .value();
    assertThat(status).isEqualTo(401);
  }

  // --- helpers

  /** A number never used before: the wrong tries from a number are remembered for an hour. */
  static String freshPhone() {
    return "519"
        + java.util.concurrent.ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
  }

  /** The code the portal shows to a signed-in person. */
  String linkCode(String subject) {
    return read(call(
            subject,
            HttpMethod.POST,
            "/v1/organizations/" + churchId + "/whatsapp/link-code",
            null))
        .get("code")
        .asText();
  }

  /** The agent tells the API that {@code from} sent {@code code}; returns the status. */
  String linkStatus(String from, String code) {
    return read(client
            .post()
            .uri("/v1/internal/whatsapp/link")
            .headers(h -> h.setBearerAuth(TestSecurityConfig.agentToken(jwtEncoder)))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                "{\"phoneNumberId\":\"%s\",\"from\":\"%s\",\"code\":\"%s\"}"
                    .formatted(phoneNumberId, from, code))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody())
        .get("status")
        .asText();
  }

  JsonNode identity(String from) {
    return read(identityBody(TestSecurityConfig.agentToken(jwtEncoder), phoneNumberId, from));
  }

  String identityBody(String token, String number, String from) {
    return client
        .post()
        .uri("/v1/internal/whatsapp/identity")
        .headers(h -> h.setBearerAuth(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"phoneNumberId\":\"%s\",\"from\":\"%s\"}".formatted(number, from))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .returnResult()
        .getResponseBody();
  }

  int identityStatus(String token, String body) {
    return client
        .post()
        .uri("/v1/internal/whatsapp/identity")
        .headers(
            h -> {
              if (token != null) {
                h.setBearerAuth(token);
              }
            })
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange()
        .returnResult(String.class)
        .getStatus()
        .value();
  }

  String createPerson(String firstName, String phone, String organizationId, String positionId) {
    var positions = positionId == null ? "[]" : "[\"%s\"]".formatted(positionId);
    return read(call(
            alice,
            HttpMethod.POST,
            "/v1/people",
            ("{\"firstName\":\"%s\",\"lastName\":\"Prueba\",\"phoneNumber\":\"%s\","
                    + "\"organizationId\":\"%s\",\"positionIds\":%s}")
                .formatted(firstName, phone, organizationId, positions)))
        .asText();
  }

  String signUp(String name) {
    var subject = "auth0|" + name + "-" + UUID.randomUUID();
    syncCurrentUserCommandHandler.handle(
        new SyncCurrentUserCommand(subject, name + "@example.org", true));
    return subject;
  }

  /** As a signed-in user (by subject). */
  String call(String subject, HttpMethod method, String uri, String body) {
    return callWith(TestSecurityConfig.validToken(jwtEncoder, subject), method, uri, body);
  }

  String callWith(String token, HttpMethod method, String uri, String body) {
    var spec = client.method(method).uri(uri).headers(h -> h.setBearerAuth(token));
    var exchanged =
        body == null
            ? spec.exchange()
            : spec.contentType(MediaType.APPLICATION_JSON).body(body).exchange();
    var result = exchanged.returnResult(String.class);
    assertThat(result.getStatus().is2xxSuccessful()).as("%s %s", method, uri).isTrue();
    return result.getResponseBody();
  }

  int status(String token, HttpMethod method, String uri) {
    return status(token, method, uri, null);
  }

  int status(String token, HttpMethod method, String uri, String body) {
    var spec = client.method(method).uri(uri).headers(h -> h.setBearerAuth(token));
    var exchanged =
        body == null
            ? spec.exchange()
            : spec.contentType(MediaType.APPLICATION_JSON).body(body).exchange();
    return exchanged.returnResult(String.class).getStatus().value();
  }

  static JsonNode read(String body) {
    try {
      return JSON.readTree(body);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
