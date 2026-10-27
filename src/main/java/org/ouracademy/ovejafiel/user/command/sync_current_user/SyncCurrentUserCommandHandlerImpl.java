package org.ouracademy.ovejafiel.user.command.sync_current_user;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.ouracademy.ovejafiel.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SyncCurrentUserCommandHandlerImpl implements SyncCurrentUserCommandHandler {

  private final UserRepository userRepository;

  @Override
  @Transactional
  public SyncCurrentUserResult handle(SyncCurrentUserCommand command) {
    var existing = userRepository.findBySubject(command.subject());
    var user = existing.orElseGet(() -> new AppUser(command.subject()));
    user.updateContact(command.email(), command.emailVerified());
    user = userRepository.save(user);
    return new SyncCurrentUserResult(UserResult.from(user), existing.isEmpty());
  }
}
