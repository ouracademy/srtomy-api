package org.ouracademy.srtomy.accountability.query.get_position_suggestions;

import java.util.List;

/** Common position names to autocomplete; a church may use any other name too. */
public record PositionSuggestionsResult(List<String> names) {}
