package org.ouracademy.ovejafiel.user.query.find_user_by_email;

import java.util.Optional;
import org.ouracademy.ovejafiel.user.query.UserResult;

public interface FindUserByEmailQueryHandler {
  Optional<UserResult> handle(FindUserByEmailQuery query);
}
