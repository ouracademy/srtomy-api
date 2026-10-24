package org.ouracademy.srtomy.accountability.query.get_position_suggestions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.accountability.query.get_position_suggestions.PositionSuggestionsResult.Suggestion;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;

class GetPositionSuggestionsQueryHandlerImplTests {

  @Test
  void suggestsNamesFromSeveralTraditions() {
    var names =
        new GetPositionSuggestionsQueryHandlerImpl()
            .handle(new GetPositionSuggestionsQuery()).suggestions().stream()
                .map(Suggestion::name)
                .toList();

    assertThat(names)
        .contains("Pastor", "Diácono", "Catequista", "Asistente")
        .doesNotHaveDuplicates();
  }

  @Test
  void eachSuggestionCarriesTheLevelItUsuallyHas() {
    var levels =
        new GetPositionSuggestionsQueryHandlerImpl()
            .handle(new GetPositionSuggestionsQuery()).suggestions().stream()
                .collect(
                    Collectors.toMap(
                        Suggestion::name, s -> Optional.ofNullable(s.suggestedRoleId())));

    assertThat(levels.get("Pastor")).contains(PredefinedRoles.ADMIN);
    assertThat(levels.get("Maestro")).contains(PredefinedRoles.LEADER);
    assertThat(levels.get("Ujier")).contains(PredefinedRoles.COLLABORATOR);
    assertThat(levels.get("Intercesor")).isEmpty();
  }

  @Test
  void neverSuggestsOwner() {
    assertThat(
            new GetPositionSuggestionsQueryHandlerImpl()
                .handle(new GetPositionSuggestionsQuery())
                .suggestions())
        .extracting(Suggestion::suggestedRoleId)
        .doesNotContain(PredefinedRoles.OWNER);
  }
}
