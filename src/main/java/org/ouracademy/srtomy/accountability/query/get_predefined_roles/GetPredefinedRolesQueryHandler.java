package org.ouracademy.srtomy.accountability.query.get_predefined_roles;

import java.util.List;
import org.ouracademy.srtomy.accountability.query.AccountabilityTypeResult;

public interface GetPredefinedRolesQueryHandler {
  List<AccountabilityTypeResult> handle(GetPredefinedRolesQuery query);
}
