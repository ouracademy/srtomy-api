package org.ouracademy.srtomy.accountability.query;

import java.util.List;

public interface GetAccountabilitiesByParentAndTypesQueryHandler {
  List<AccountabilityResult> handle(GetAccountabilitiesByParentAndTypesQuery query);
}
