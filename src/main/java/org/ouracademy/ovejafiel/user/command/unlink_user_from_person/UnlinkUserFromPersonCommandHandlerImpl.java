package org.ouracademy.ovejafiel.user.command.unlink_user_from_person;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.user.repository.UserPersonLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UnlinkUserFromPersonCommandHandlerImpl implements UnlinkUserFromPersonCommandHandler {

  private final UserPersonLinkRepository userPersonLinkRepository;

  @Override
  @Transactional
  public UnlinkUserFromPersonResult handle(UnlinkUserFromPersonCommand command) {
    return new UnlinkUserFromPersonResult(
        userPersonLinkRepository.deleteByPersonId(command.personId()) > 0);
  }
}
