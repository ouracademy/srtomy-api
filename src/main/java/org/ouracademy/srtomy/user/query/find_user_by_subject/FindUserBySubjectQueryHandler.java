package org.ouracademy.srtomy.user.query.find_user_by_subject;

import java.util.Optional;
import org.ouracademy.srtomy.user.query.UserResult;

public interface FindUserBySubjectQueryHandler {
  Optional<UserResult> handle(FindUserBySubjectQuery query);
}
