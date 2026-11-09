package org.ouracademy.ovejafiel.user.query.find_user_by_id;

import java.util.Optional;
import org.ouracademy.ovejafiel.user.query.UserResult;

public interface FindUserByIdQueryHandler {
  Optional<UserResult> handle(FindUserByIdQuery query);
}
