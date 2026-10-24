package org.ouracademy.srtomy.accountability.query.get_positions_of_church;

import java.util.List;
import org.ouracademy.srtomy.accountability.query.PositionResult;

public interface GetPositionsOfChurchQueryHandler {
  List<PositionResult> handle(GetPositionsOfChurchQuery query);
}
