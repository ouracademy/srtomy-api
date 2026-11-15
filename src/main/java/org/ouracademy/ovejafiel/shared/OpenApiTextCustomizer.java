package org.ouracademy.ovejafiel.shared;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.stereotype.Component;

/**
 * The summaries and descriptions of the OpenAPI document come from the Javadoc of the controllers
 * and their requests, so they arrive wrapped at the line length of the code and with the HTML of
 * {@code {@code ...}}. This leaves them as plain one-line text, which is what its readers want: the
 * AI agent that turns each operation into a tool, and the generated client of the web app.
 */
@Component
public class OpenApiTextCustomizer implements GlobalOpenApiCustomizer {

  @Override
  public void customise(OpenAPI openApi) {
    if (openApi.getPaths() != null) {
      openApi.getPaths().values().stream()
          .flatMap(path -> path.readOperations().stream())
          .forEach(OpenApiTextCustomizer::clean);
    }
    if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
      Set<Schema<?>> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
      openApi.getComponents().getSchemas().values().forEach(schema -> clean(schema, seen));
    }
  }

  private static void clean(Operation operation) {
    operation.setSummary(plain(operation.getSummary()));
    operation.setDescription(plain(operation.getDescription()));
    if (operation.getParameters() != null) {
      operation
          .getParameters()
          .forEach(parameter -> parameter.setDescription(plain(parameter.getDescription())));
    }
  }

  @SuppressWarnings("rawtypes")
  private static void clean(Schema<?> schema, Set<Schema<?>> seen) {
    if (schema == null || !seen.add(schema)) {
      return;
    }
    schema.setDescription(plain(schema.getDescription()));
    clean(schema.getItems(), seen);
    Map<String, Schema> properties = schema.getProperties();
    if (properties != null) {
      Collection<Schema> values = properties.values();
      values.forEach(property -> clean(property, seen));
    }
  }

  /** One line, with code and links in backquotes instead of HTML; null stays null. */
  static String plain(String text) {
    if (text == null) {
      return null;
    }
    return text.replaceAll("\\s+", " ")
        .replaceAll("<code>\\s*", "`")
        .replaceAll("\\s*</code>", "`")
        .replaceAll("</?p>", "")
        // What is left of a link to a class or a method: its name.
        .replaceAll("\\{@link\\s+(?:\\S+\\s+)?#?([^}\\s]+)}", "`$1`")
        .trim();
  }
}
