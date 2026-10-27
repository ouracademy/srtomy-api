package org.ouracademy.ovejafiel.accountability.controller.dto;

import java.util.List;
import org.ouracademy.ovejafiel.accountability.query.get_position_suggestions.PositionSuggestionsResult.Suggestion;

/** A common position name with the permissions it usually gives (empty for a plain label). */
public record PositionSuggestionResponse(String name, List<String> permissions) {

  public static PositionSuggestionResponse from(Suggestion suggestion) {
    return new PositionSuggestionResponse(suggestion.name(), suggestion.permissions());
  }
}
