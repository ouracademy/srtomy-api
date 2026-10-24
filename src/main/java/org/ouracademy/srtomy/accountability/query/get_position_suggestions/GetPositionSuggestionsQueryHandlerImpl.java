package org.ouracademy.srtomy.accountability.query.get_position_suggestions;

import java.util.List;
import org.ouracademy.srtomy.accountability.query.get_position_suggestions.PositionSuggestionsResult.Suggestion;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.springframework.stereotype.Service;

@Service
class GetPositionSuggestionsQueryHandlerImpl implements GetPositionSuggestionsQueryHandler {

  private static final IamRoleId ADMIN = PredefinedRoles.ADMIN;
  private static final IamRoleId LEADER = PredefinedRoles.LEADER;
  private static final IamRoleId COLLABORATOR = PredefinedRoles.COLLABORATOR;

  /**
   * Names used across traditions (evangelical, catholic, ...) with the access level they usually
   * have. Only suggestions: nothing validates a position against this list.
   */
  static final List<Suggestion> SUGGESTIONS =
      List.of(
          new Suggestion("Pastor", ADMIN),
          new Suggestion("Pastora", ADMIN),
          new Suggestion("Copastor", ADMIN),
          new Suggestion("Obispo", ADMIN),
          new Suggestion("Apóstol", ADMIN),
          new Suggestion("Sacerdote", ADMIN),
          new Suggestion("Párroco", ADMIN),
          new Suggestion("Anciano", LEADER),
          new Suggestion("Diácono", LEADER),
          new Suggestion("Diaconisa", LEADER),
          new Suggestion("Evangelista", LEADER),
          new Suggestion("Misionero", LEADER),
          new Suggestion("Catequista", LEADER),
          new Suggestion("Líder de alabanza", LEADER),
          new Suggestion("Maestro", LEADER),
          new Suggestion("Maestra de escuela dominical", LEADER),
          new Suggestion("Servidor", COLLABORATOR),
          new Suggestion("Ujier", COLLABORATOR),
          new Suggestion("Asistente", COLLABORATOR),
          new Suggestion("Tesorero", COLLABORATOR),
          new Suggestion("Secretario", COLLABORATOR),
          new Suggestion("Ministro de la Eucaristía", COLLABORATOR),
          new Suggestion("Músico", null),
          new Suggestion("Intercesor", null),
          new Suggestion("Hermano servidor", null),
          new Suggestion("Hermana servidora", null));

  @Override
  public PositionSuggestionsResult handle(GetPositionSuggestionsQuery query) {
    return new PositionSuggestionsResult(SUGGESTIONS);
  }
}
