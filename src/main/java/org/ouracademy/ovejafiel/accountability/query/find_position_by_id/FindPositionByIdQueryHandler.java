package org.ouracademy.ovejafiel.accountability.query.find_position_by_id;

import java.util.Optional;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;

public interface FindPositionByIdQueryHandler {
  Optional<PositionResult> handle(FindPositionByIdQuery query);
}
