package org.ouracademy.srtomy.accountability.controller.dto;

import java.util.UUID;
import org.ouracademy.srtomy.accountability.query.get_position_suggestions.PositionSuggestionsResult.Suggestion;

/** A common position name with the access level it usually has (null for a plain label). */
public record PositionSuggestionResponse(String name, UUID suggestedRoleId) {

  public static PositionSuggestionResponse from(Suggestion suggestion) {
    return new PositionSuggestionResponse(
        suggestion.name(),
        suggestion.suggestedRoleId() == null ? null : suggestion.suggestedRoleId().value());
  }
}
