package org.ouracademy.ovejafiel.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The OpenAPI document is what the AI agent reads to know what each endpoint is for: an operation
 * without a description is a tool the model has to guess. These tests fail when an endpoint, one of
 * its parameters or a field of its request body is published without one. The text comes from the
 * Javadoc of the controller method, its {@code @param} tags and the fields of the request.
 *
 * <p>They also leave the document in {@code target/openapi/srtomy-api.json}, to copy to the
 * projects that keep a copy (srtomy-ui, srtomy-agent) without starting the application.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
class OpenApiDocumentationIntegrationTests {

  /** Called by Google and by providers, never by a person or the agent. */
  private static final List<String> NOT_FOR_PEOPLE = List.of("/v1/internal/", "/v1/webhooks/");

  private static final Set<String> METHODS = Set.of("get", "post", "put", "patch", "delete");

  private static final Path OUTPUT = Path.of("target", "openapi", "srtomy-api.json");

  @LocalServerPort int port;
  @Autowired JwtEncoder jwtEncoder;
  JsonNode spec;

  @BeforeEach
  void readTheDocument() throws IOException {
    var body =
        RestTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build()
            .get()
            .uri("/v3/api-docs")
            // Outside the local profile the document is not public.
            .headers(headers -> headers.setBearerAuth(TestSecurityConfig.validToken(jwtEncoder)))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();
    var mapper = new ObjectMapper();
    spec = mapper.readTree(body);
    Files.createDirectories(OUTPUT.getParent());
    Files.writeString(OUTPUT, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec));
  }

  @Test
  void everyOperationSaysWhatItIsFor() {
    var missing = new ArrayList<String>();
    forEachOperation(
        (name, operation) -> {
          if (isBlank(operation.path("summary")) || isBlank(operation.path("description"))) {
            missing.add(name);
          }
        });

    assertThat(missing).as("operations without a Javadoc comment on their method").isEmpty();
  }

  @Test
  void theTextIsPlainWithoutTheLineBreaksOrTheHtmlOfJavadoc() {
    var marked = new ArrayList<String>();
    forEachOperation(
        (name, operation) -> {
          var text = operation.path("summary").asString("") + operation.path("description");
          if (text.contains("<code>") || text.contains("\\n") || text.contains("\n")) {
            marked.add(name);
          }
        });

    assertThat(marked).isEmpty();
  }

  @Test
  void everyParameterIsDescribed() {
    var missing = new ArrayList<String>();
    forEachOperation(
        (name, operation) ->
            operation
                .path("parameters")
                .forEach(
                    parameter -> {
                      if (isBlank(parameter.path("description"))) {
                        missing.add(name + " " + parameter.path("name").asString());
                      }
                    }));

    assertThat(missing).as("parameters without a @param tag on their method").isEmpty();
  }

  @Test
  void everyFieldOfARequestBodyIsDescribed() {
    var missing = new ArrayList<String>();
    forEachOperation(
        (name, operation) -> {
          var schema =
              operation.path("requestBody").path("content").path("application/json").path("schema");
          undescribedFields(schema, "", 0, missing, name);
        });

    assertThat(missing.stream().distinct().toList())
        .as("fields of a request without a Javadoc comment")
        .isEmpty();
  }

  private void undescribedFields(
      JsonNode schema, String prefix, int depth, List<String> missing, String operation) {
    var resolved = resolve(schema);
    if (depth > 6 || resolved.isMissingNode()) {
      return;
    }
    if (resolved.has("items")) {
      undescribedFields(resolved.path("items"), prefix + "[]", depth + 1, missing, operation);
    }
    for (var field : resolved.path("properties").properties()) {
      var path = prefix.isEmpty() ? field.getKey() : prefix + "." + field.getKey();
      // The description may sit on the field or on the type it refers to.
      if (isBlank(field.getValue().path("description"))
          && isBlank(resolve(field.getValue()).path("description"))) {
        missing.add(schemaName(schema) + " " + path + "  (" + operation + ")");
      }
      undescribedFields(field.getValue(), path, depth + 1, missing, operation);
    }
  }

  private JsonNode resolve(JsonNode schema) {
    if (schema.has("$ref")) {
      return spec.path("components").path("schemas").path(schemaName(schema));
    }
    return schema;
  }

  private static String schemaName(JsonNode schema) {
    var ref = schema.path("$ref").asString("");
    return ref.substring(ref.lastIndexOf('/') + 1);
  }

  private void forEachOperation(java.util.function.BiConsumer<String, JsonNode> action) {
    for (var path : spec.path("paths").properties()) {
      if (NOT_FOR_PEOPLE.stream().anyMatch(path.getKey()::startsWith)) {
        continue;
      }
      for (var method : path.getValue().properties()) {
        if (METHODS.contains(method.getKey())) {
          action.accept(method.getKey().toUpperCase() + " " + path.getKey(), method.getValue());
        }
      }
    }
  }

  private static boolean isBlank(JsonNode node) {
    return node.asString("").isBlank();
  }
}
