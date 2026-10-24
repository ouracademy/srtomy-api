package org.ouracademy.srtomy.accountability.query.find_position_by_id;

import java.util.Optional;
import org.ouracademy.srtomy.accountability.query.PositionResult;

public interface FindPositionByIdQueryHandler {
  Optional<PositionResult> handle(FindPositionByIdQuery query);
}
