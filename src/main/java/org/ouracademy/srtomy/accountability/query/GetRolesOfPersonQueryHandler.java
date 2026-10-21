package org.ouracademy.srtomy.accountability.query;

import java.util.List;

public interface GetRolesOfPersonQueryHandler {
  List<AccountabilityTypeResult> handle(GetRolesOfPersonQuery query);
}
