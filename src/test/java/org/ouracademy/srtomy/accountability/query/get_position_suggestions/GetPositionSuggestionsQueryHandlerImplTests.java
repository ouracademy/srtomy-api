package org.ouracademy.srtomy.accountability.query.get_position_suggestions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.accountability.query.get_position_suggestions.PositionSuggestionsResult.Suggestion;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.query.expand_permissions.ExpandPermissionsQuery;
import org.ouracademy.srtomy.iam.query.expand_permissions.ExpandPermissionsQueryHandler;
import org.ouracademy.srtomy.iam.query.expand_permissions.ExpandPermissionsResult;

class GetPositionSuggestionsQueryHandlerImplTests {

  ExpandPermissionsQueryHandler expand = mock(ExpandPermissionsQueryHandler.class);
  GetPositionSuggestionsQueryHandlerImpl handler =
      new GetPositionSuggestionsQueryHandlerImpl(expand);

  /** Gives every role one permission named after its id, so each template is recognizable. */
  void expandRolesToTheirIds() {
    when(expand.handle(any()))
        .thenAnswer(
            call -> {
              ExpandPermissionsQuery query = call.getArgument(0);
              return new ExpandPermissionsResult(
                  query.roleIds().stream().map(id -> id.value().toString()).sorted().toList());
            });
  }

  @Test
  void suggestsNamesFromSeveralTraditions() {
    expandRolesToTheirIds();

    var names =
        handler.handle(new GetPositionSuggestionsQuery()).suggestions().stream()
            .map(Suggestion::name)
            .toList();

    assertThat(names)
        .contains("Pastor", "Diácono", "Catequista", "Asistente")
        .doesNotHaveDuplicates();
  }

  @Test
  void eachSuggestionCarriesThePermissionsOfItsBaseRoles() {
    expandRolesToTheirIds();

    var byName =
        handler.handle(new GetPositionSuggestionsQuery()).suggestions().stream()
            .collect(Collectors.toMap(Suggestion::name, Suggestion::permissions));

    assertThat(byName.get("Pastor")).containsExactly(PredefinedRoles.ADMIN.value().toString());
    assertThat(byName.get("Ujier"))
        .containsExactlyInAnyOrder(
            PredefinedRoles.ATTENDANCE.value().toString(),
            PredefinedRoles.VIEW_PEOPLE.value().toString(),
            PredefinedRoles.MEMBER.value().toString());
    assertThat(byName.get("Secretario")).hasSize(2);
  }

  @Test
  void labelsGiveNothing() {
    expandRolesToTheirIds();

    var byName =
        handler.handle(new GetPositionSuggestionsQuery()).suggestions().stream()
            .collect(Collectors.toMap(Suggestion::name, Suggestion::permissions));

    assertThat(byName.get("Intercesor")).isEqualTo(List.of());
    assertThat(byName.get("Hermano servidor")).isEqualTo(List.of());
  }

  @Test
  void neverSuggestsOwner() {
    expandRolesToTheirIds();

    assertThat(
            handler.handle(new GetPositionSuggestionsQuery()).suggestions().stream()
                .flatMap(s -> s.permissions().stream()))
        .doesNotContain(PredefinedRoles.OWNER.value().toString());
  }
}
