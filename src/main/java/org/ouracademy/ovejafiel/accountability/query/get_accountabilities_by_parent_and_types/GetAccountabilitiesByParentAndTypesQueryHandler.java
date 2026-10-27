package org.ouracademy.ovejafiel.accountability.query.get_accountabilities_by_parent_and_types;

import java.util.List;

public interface GetAccountabilitiesByParentAndTypesQueryHandler {
  List<AccountabilityResult> handle(GetAccountabilitiesByParentAndTypesQuery query);
}
