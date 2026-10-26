package org.ouracademy.ovejafiel.accountability.query.get_positions_of_person;

import java.util.List;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;

public interface GetPositionsOfPersonQueryHandler {
  List<PositionResult> handle(GetPositionsOfPersonQuery query);
}
