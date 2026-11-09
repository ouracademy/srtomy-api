package org.ouracademy.ovejafiel.user.query.find_user_by_id;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.ouracademy.ovejafiel.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindUserByIdQueryHandlerImpl implements FindUserByIdQueryHandler {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<UserResult> handle(FindUserByIdQuery query) {
    return userRepository.findById(query.id()).map(UserResult::from);
  }
}
