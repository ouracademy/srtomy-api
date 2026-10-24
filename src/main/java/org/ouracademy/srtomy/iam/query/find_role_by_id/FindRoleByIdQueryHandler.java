package org.ouracademy.srtomy.iam.query.find_role_by_id;

import java.util.Optional;
import org.ouracademy.srtomy.iam.query.RoleResult;

public interface FindRoleByIdQueryHandler {
  Optional<RoleResult> handle(FindRoleByIdQuery query);
}
