package org.ouracademy.srtomy.accountability.query.get_position_suggestions;

import java.util.List;
import org.ouracademy.srtomy.iam.domain.IamRoleId;

/**
 * Common position names to autocomplete, each with the access level it usually has ({@code
 * suggestedRoleId}, null for a plain label). A church may use any other name and any level.
 */
public record PositionSuggestionsResult(List<Suggestion> suggestions) {

  public record Suggestion(String name, IamRoleId suggestedRoleId) {}
}
