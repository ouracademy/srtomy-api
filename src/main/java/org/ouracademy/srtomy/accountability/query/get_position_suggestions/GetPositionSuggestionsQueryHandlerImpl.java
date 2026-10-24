package org.ouracademy.srtomy.accountability.query.get_position_suggestions;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
class GetPositionSuggestionsQueryHandlerImpl implements GetPositionSuggestionsQueryHandler {

  /**
   * Names used across traditions (evangelical, catholic, ...). Only suggestions: nothing validates
   * a position against this list.
   */
  static final List<String> SUGGESTIONS =
      List.of(
          "Pastor",
          "Pastora",
          "Copastor",
          "Anciano",
          "Diácono",
          "Diaconisa",
          "Obispo",
          "Apóstol",
          "Evangelista",
          "Misionero",
          "Sacerdote",
          "Párroco",
          "Catequista",
          "Ministro de la Eucaristía",
          "Líder de alabanza",
          "Músico",
          "Maestro",
          "Maestra de escuela dominical",
          "Servidor",
          "Ujier",
          "Asistente",
          "Hermano servidor",
          "Hermana servidora",
          "Tesorero",
          "Secretario",
          "Intercesor");

  @Override
  public PositionSuggestionsResult handle(GetPositionSuggestionsQuery query) {
    return new PositionSuggestionsResult(SUGGESTIONS);
  }
}
