package org.ouracademy.ovejafiel.user.query.find_user_by_subject;

import java.util.Optional;
import org.ouracademy.ovejafiel.user.query.UserResult;

public interface FindUserBySubjectQueryHandler {
  Optional<UserResult> handle(FindUserBySubjectQuery query);
}
