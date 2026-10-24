package org.ouracademy.srtomy.accountability.query.get_position_suggestions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GetPositionSuggestionsQueryHandlerImplTests {

  @Test
  void suggestsNamesFromSeveralTraditions() {
    var names =
        new GetPositionSuggestionsQueryHandlerImpl()
            .handle(new GetPositionSuggestionsQuery())
            .names();

    assertThat(names)
        .contains("Pastor", "Diácono", "Catequista", "Asistente")
        .doesNotHaveDuplicates();
  }
}
