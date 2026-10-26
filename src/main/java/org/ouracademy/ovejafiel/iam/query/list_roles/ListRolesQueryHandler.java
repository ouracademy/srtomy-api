package org.ouracademy.ovejafiel.iam.query.list_roles;

import java.util.List;
import org.ouracademy.ovejafiel.iam.query.RoleResult;

public interface ListRolesQueryHandler {
  List<RoleResult> handle(ListRolesQuery query);
}
