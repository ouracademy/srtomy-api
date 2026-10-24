package org.ouracademy.srtomy.user.query.find_user_by_email;

import java.util.Optional;
import org.ouracademy.srtomy.user.query.UserResult;

public interface FindUserByEmailQueryHandler {
  Optional<UserResult> handle(FindUserByEmailQuery query);
}
