package org.ouracademy.srtomy.user.query.find_user_by_subject;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.user.query.UserResult;
import org.ouracademy.srtomy.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindUserBySubjectQueryHandlerImpl implements FindUserBySubjectQueryHandler {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<UserResult> handle(FindUserBySubjectQuery query) {
    return userRepository.findBySubject(query.subject()).map(UserResult::from);
  }
}
