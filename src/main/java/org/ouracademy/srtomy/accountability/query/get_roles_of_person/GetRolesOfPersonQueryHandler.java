package org.ouracademy.srtomy.accountability.query.get_roles_of_person;

import java.util.List;
import org.ouracademy.srtomy.accountability.query.AccountabilityTypeResult;

public interface GetRolesOfPersonQueryHandler {
  List<AccountabilityTypeResult> handle(GetRolesOfPersonQuery query);
}
