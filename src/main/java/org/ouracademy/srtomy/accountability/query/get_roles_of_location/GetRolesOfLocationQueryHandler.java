package org.ouracademy.srtomy.accountability.query.get_roles_of_location;

import java.util.List;
import org.ouracademy.srtomy.accountability.query.AccountabilityTypeResult;

public interface GetRolesOfLocationQueryHandler {
  List<AccountabilityTypeResult> handle(GetRolesOfLocationQuery query);
}
