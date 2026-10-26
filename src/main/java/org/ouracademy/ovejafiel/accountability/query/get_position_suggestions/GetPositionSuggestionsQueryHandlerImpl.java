package org.ouracademy.ovejafiel.accountability.query.get_position_suggestions;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.query.get_position_suggestions.PositionSuggestionsResult.Suggestion;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.iam.query.expand_permissions.ExpandPermissionsQuery;
import org.ouracademy.ovejafiel.iam.query.expand_permissions.ExpandPermissionsQueryHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class GetPositionSuggestionsQueryHandlerImpl implements GetPositionSuggestionsQueryHandler {

  private final ExpandPermissionsQueryHandler expandPermissionsQueryHandler;

  /** A common name and the predefined roles whose permissions it usually gives. */
  record Template(String name, List<IamRoleId> roles) {
    static Template of(String name, IamRoleId... roles) {
      return new Template(name, List.of(roles));
    }
  }

  private static final IamRoleId ADMIN = PredefinedRoles.ADMIN;
  private static final IamRoleId LEADER = PredefinedRoles.LEADER;

  /**
   * Names used across traditions (evangelical, catholic, ...). Only suggestions: nothing validates
   * a position against this list, and the church gets a copy of the permissions, not a link.
   */
  static final List<Template> TEMPLATES =
      List.of(
          Template.of("Pastor", ADMIN),
          Template.of("Pastora", ADMIN),
          Template.of("Copastor", ADMIN),
          Template.of("Obispo", ADMIN),
          Template.of("Apóstol", ADMIN),
          Template.of("Sacerdote", ADMIN),
          Template.of("Párroco", ADMIN),
          Template.of("Anciano", LEADER),
          Template.of("Diácono", LEADER),
          Template.of("Diaconisa", LEADER),
          Template.of("Evangelista", LEADER),
          Template.of("Misionero", LEADER),
          Template.of("Líder de alabanza", LEADER),
          Template.of("Líder de grupo", LEADER),
          Template.of(
              "Maestro",
              PredefinedRoles.EVENTS_MANAGER,
              PredefinedRoles.ATTENDANCE,
              PredefinedRoles.VIEW_PEOPLE),
          Template.of(
              "Maestra de escuela dominical",
              PredefinedRoles.EVENTS_MANAGER,
              PredefinedRoles.ATTENDANCE,
              PredefinedRoles.VIEW_PEOPLE),
          Template.of(
              "Catequista",
              PredefinedRoles.EVENTS_MANAGER,
              PredefinedRoles.ATTENDANCE,
              PredefinedRoles.VIEW_PEOPLE),
          Template.of("Secretario", PredefinedRoles.PEOPLE_MANAGER, PredefinedRoles.MEMBER),
          Template.of(
              "Ujier",
              PredefinedRoles.ATTENDANCE,
              PredefinedRoles.VIEW_PEOPLE,
              PredefinedRoles.MEMBER),
          Template.of(
              "Asistente",
              PredefinedRoles.ATTENDANCE,
              PredefinedRoles.VIEW_PEOPLE,
              PredefinedRoles.MEMBER),
          Template.of("Tesorero", PredefinedRoles.MEMBER),
          Template.of("Servidor", PredefinedRoles.MEMBER),
          Template.of("Ministro de la Eucaristía", PredefinedRoles.MEMBER),
          Template.of("Músico"),
          Template.of("Intercesor"),
          Template.of("Hermano servidor"),
          Template.of("Hermana servidora"));

  @Override
  public PositionSuggestionsResult handle(GetPositionSuggestionsQuery query) {
    return new PositionSuggestionsResult(
        TEMPLATES.stream()
            .map(
                template ->
                    new Suggestion(
                        template.name(),
                        expandPermissionsQueryHandler
                            .handle(new ExpandPermissionsQuery(template.roles(), List.of()))
                            .permissions()))
            .toList());
  }
}
