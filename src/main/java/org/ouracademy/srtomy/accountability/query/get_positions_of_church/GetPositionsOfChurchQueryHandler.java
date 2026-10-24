package org.ouracademy.srtomy.accountability.query.get_positions_of_church;

import java.util.List;
import org.ouracademy.srtomy.accountability.query.PositionSummaryResult;

public interface GetPositionsOfChurchQueryHandler {
  List<PositionSummaryResult> handle(GetPositionsOfChurchQuery query);
}
