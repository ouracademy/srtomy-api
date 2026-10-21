package org.ouracademy.srtomy.accountability.query;

import java.util.List;

public interface GetPredefinedRolesQueryHandler {
  List<AccountabilityTypeResult> handle(GetPredefinedRolesQuery query);
}
