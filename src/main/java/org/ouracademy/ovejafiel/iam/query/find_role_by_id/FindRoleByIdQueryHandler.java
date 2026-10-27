package org.ouracademy.ovejafiel.iam.query.find_role_by_id;

import java.util.Optional;
import org.ouracademy.ovejafiel.iam.query.RoleResult;

public interface FindRoleByIdQueryHandler {
  Optional<RoleResult> handle(FindRoleByIdQuery query);
}
