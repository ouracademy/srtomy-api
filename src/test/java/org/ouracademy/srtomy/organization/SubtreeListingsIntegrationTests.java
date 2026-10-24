package org.ouracademy.srtomy.organization;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

/** People and events of a node include everything below it, labelled by where they come from. */
@SpringBootTest(classes = SrtomyApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class SubtreeListingsIntegrationTests {

  static final ObjectMapper JSON = new ObjectMapper();

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  @Autowired SyncCurrentUserCommandHandler syncCurrentUserCommandHandler;

  RestTestClient client;
  String alice;
  String church;
  String location;
  String group;

  @BeforeEach
  void setUp() {
    client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port + "/v1").build();
    alice = "auth0|alice-" + UUID.randomUUID();
    syncCurrentUserCommandHandler.handle(
        new SyncCurrentUserCommand(alice, alice.replace("auth0|", "") + "@example.org", true));
    church =
        read(post(
                "/organizations",
                "{\"name\":\"Iglesia\",\"location\":{\"name\":\"Sede\"},\"positions\":[]}"))
            .get("id")
            .asText();
    location = get("/organizations/" + church + "/childs").get(0).get("id").asText();
    group =
        read(post(
                "/organizations/locations/childs",
                "{\"name\":\"Jóvenes\",\"type\":\"GROUP\",\"parentId\":\"%s\"}"
                    .formatted(location)))
            .get("id")
            .asText();
  }

  @Test
  void aNodeListsThePeopleOfItsDescendantsLabelledWithTheirOrigin() {
    createPerson("Ana", location);
    createPerson("Beto", group);

    var fromLocation = get("/people?organizationId=" + location).get("content");
    assertThat(values(fromLocation, "firstName")).containsExactlyInAnyOrder("Ana", "Beto");
    var beto = find(fromLocation, "Beto");
    assertThat(beto.get("organizationName").asText()).isEqualTo("Jóvenes");
    assertThat(beto.get("directMember").asBoolean()).isFalse();
    assertThat(find(fromLocation, "Ana").get("directMember").asBoolean()).isTrue();

    // The church sees them too, and the group does not see its parent's people.
    assertThat(get("/people?organizationId=" + church).get("content")).hasSize(2);
    assertThat(values(get("/people?organizationId=" + group).get("content"), "firstName"))
        .containsExactly("Beto");
    assertThat(
            get("/people?organizationId=" + location + "&searchText=bet")
                .get("totalElements")
                .asInt())
        .isEqualTo(1);
  }

  @Test
  void aNodeListsTheEventsOfItsDescendants() {
    createEvent("Culto", location);
    createEvent("Reunión de jóvenes", group);

    var url = "/events?start=2030-01-01T00:00:00&end=2030-01-02T00:00:00&organizationId=";
    assertThat(values(get(url + location), "title"))
        .containsExactlyInAnyOrder("Culto", "Reunión de jóvenes");
    assertThat(values(get(url + group), "title")).containsExactly("Reunión de jóvenes");
  }

  // --- helpers

  void createPerson(String firstName, String organizationId) {
    post(
        "/people",
        "{\"firstName\":\"%s\",\"lastName\":\"Diaz\",\"organizationId\":\"%s\"}"
            .formatted(firstName, organizationId));
  }

  void createEvent(String name, String organizationId) {
    post(
        "/events",
        ("{\"name\":\"%s\",\"startTime\":\"2030-01-01T10:00:00Z\","
                + "\"endTime\":\"2030-01-01T11:00:00Z\",\"organizationId\":\"%s\"}")
            .formatted(name, organizationId));
  }

  static JsonNode find(JsonNode array, String firstName) {
    for (var node : array) {
      if (firstName.equals(node.get("firstName").asText())) {
        return node;
      }
    }
    throw new AssertionError("No person " + firstName + " in " + array);
  }

  static List<String> values(JsonNode array, String field) {
    var values = new ArrayList<String>();
    array.forEach(node -> values.add(node.get(field).asText()));
    return values;
  }

  String post(String uri, String body) {
    return client
        .method(HttpMethod.POST)
        .uri(uri)
        .headers(h -> h.setBearerAuth(TestSecurityConfig.validToken(jwtEncoder, alice)))
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange()
        .expectStatus()
        .is2xxSuccessful()
        .expectBody(String.class)
        .returnResult()
        .getResponseBody();
  }

  JsonNode get(String uri) {
    return read(
        client
            .get()
            .uri(uri)
            .headers(h -> h.setBearerAuth(TestSecurityConfig.validToken(jwtEncoder, alice)))
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
}
