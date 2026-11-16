package org.ouracademy.ovejafiel.shared;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
 * Base of the tests that go through the real API and database: each test starts with a new church
 * whose owner is {@code owner}, with one location.
 */
@SpringBootTest(classes = OvejafielApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
public abstract class ChurchApiTests {

  protected static final ObjectMapper JSON = new ObjectMapper();

  /** What a call answered. */
  public record Answer(int status, JsonNode body) {}

  @LocalServerPort int port;
  @Autowired protected JwtEncoder jwtEncoder;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;

  protected RestTestClient client;
  protected String owner;
  protected String church;
  protected String location;

  @BeforeEach
  void createChurch() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port + "/v1").build();
    owner = newUser("owner");
    church =
        post(
                "/organizations",
                "{\"name\":\"Iglesia\",\"location\":{\"name\":\"Sede\"},\"positions\":[],"
                    + "\"creator\":{\"firstName\":\"Pablo\",\"lastName\":\"Diaz\"}}")
            .get("id")
            .asText();
    location = get("/organizations/" + church + "/childs").get(0).get("id").asText();
  }

  /** A signed-in user with no access to anything; returns its subject. */
  protected String newUser(String name) {
    var subject = "auth0|" + name + "-" + UUID.randomUUID();
    syncCurrentUserCommandHandler.handle(
        new SyncCurrentUserCommand(subject, subject.replace("auth0|", "") + "@example.org", true));
    return subject;
  }

  /** Creates a person in {@code organizationId} and returns its id. */
  protected String createPerson(String firstName, String organizationId) {
    return createPerson(firstName, null, organizationId);
  }

  protected String createPerson(String firstName, String email, String organizationId) {
    var emailField = email == null ? "" : ",\"email\":\"" + email + "\"";
    // The API answers with the bare id.
    return post(
            "/people",
            "{\"firstName\":\"%s\",\"lastName\":\"Diaz\",\"organizationId\":\"%s\"%s}"
                .formatted(firstName, organizationId, emailField))
        .asText();
  }

  protected String createEvent(String name, String organizationId) {
    return post(
            "/events",
            ("{\"name\":\"%s\",\"startTime\":\"2030-01-06T10:00:00Z\","
                    + "\"endTime\":\"2030-01-06T11:00:00Z\",\"organizationId\":\"%s\"}")
                .formatted(name, organizationId))
        .get("id")
        .asText();
  }

  protected JsonNode get(String uri) {
    return ok(call(HttpMethod.GET, uri, null, owner));
  }

  protected JsonNode post(String uri, String body) {
    return ok(call(HttpMethod.POST, uri, body, owner));
  }

  protected JsonNode put(String uri, String body) {
    return ok(call(HttpMethod.PUT, uri, body, owner));
  }

  protected JsonNode delete(String uri) {
    return ok(call(HttpMethod.DELETE, uri, null, owner));
  }

  private static JsonNode ok(Answer answer) {
    if (answer.status() < 200 || answer.status() > 299) {
      throw new AssertionError("Expected 2xx but got " + answer.status() + ": " + answer.body());
    }
    return answer.body();
  }

  /** Any call, as {@code subject} (anonymous when null), whatever it answers. */
  protected Answer call(HttpMethod method, String uri, String body, String subject) {
    var request = client.method(method).uri(uri);
    if (subject != null) {
      request.headers(h -> h.setBearerAuth(TestSecurityConfig.validToken(jwtEncoder, subject)));
    }
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).body(body);
    }
    var result = request.exchange().expectBody(String.class).returnResult();
    var text = result.getResponseBody();
    try {
      return new Answer(
          result.getStatus().value(),
          text == null || text.isBlank() ? JSON.nullNode() : JSON.readTree(text));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
