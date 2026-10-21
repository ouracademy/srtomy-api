package org.ouracademy.srtomy.accountability.query;

import java.util.List;

public interface GetRolesOfLocationQueryHandler {
  List<AccountabilityTypeResult> handle(GetRolesOfLocationQuery query);
}
