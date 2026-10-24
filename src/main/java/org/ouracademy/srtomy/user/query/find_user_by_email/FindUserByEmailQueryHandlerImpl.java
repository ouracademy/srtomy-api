package org.ouracademy.srtomy.user.query.find_user_by_email;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.user.query.UserResult;
import org.ouracademy.srtomy.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindUserByEmailQueryHandlerImpl implements FindUserByEmailQueryHandler {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<UserResult> handle(FindUserByEmailQuery query) {
    if (query.email() == null || query.email().isBlank()) {
      return Optional.empty();
    }
    return userRepository
        .findFirstByEmailIgnoreCaseAndEmailVerifiedTrueOrderByCreateDate(query.email().trim())
        .map(UserResult::from);
  }
}
