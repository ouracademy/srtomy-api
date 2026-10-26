package org.ouracademy.ovejafiel.accountability.query.get_positions_of_church;

import java.util.List;
import org.ouracademy.ovejafiel.accountability.query.PositionSummaryResult;

public interface GetPositionsOfChurchQueryHandler {
  List<PositionSummaryResult> handle(GetPositionsOfChurchQuery query);
}
