package org.ouracademy.ovejafiel.iam.query.list_predefined_roles;

import java.util.List;
import org.ouracademy.ovejafiel.iam.query.RoleResult;

/** The predefined access levels, Owner included (check {@code assignable}). */
public interface ListPredefinedRolesQueryHandler {
  List<RoleResult> handle(ListPredefinedRolesQuery query);
}
