package org.ouracademy.srtomy.iam.query.list_roles;

import java.util.List;
import org.ouracademy.srtomy.iam.query.RoleResult;

public interface ListRolesQueryHandler {
  List<RoleResult> handle(ListRolesQuery query);
}
