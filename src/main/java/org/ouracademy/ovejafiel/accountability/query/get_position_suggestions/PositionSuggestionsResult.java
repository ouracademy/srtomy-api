package org.ouracademy.ovejafiel.accountability.query.get_position_suggestions;

import java.util.List;

/**
 * Common position names to autocomplete, each with the permissions it usually gives (empty for a
 * plain label). A church may use any other name and any permissions.
 */
public record PositionSuggestionsResult(List<Suggestion> suggestions) {

  public record Suggestion(String name, List<String> permissions) {}
}
