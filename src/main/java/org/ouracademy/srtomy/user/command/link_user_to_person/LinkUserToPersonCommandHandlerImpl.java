package org.ouracademy.srtomy.user.command.link_user_to_person;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.Party;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.ouracademy.srtomy.user.domain.AppUser;
import org.ouracademy.srtomy.user.domain.UserPersonLink;
import org.ouracademy.srtomy.user.repository.UserPersonLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class LinkUserToPersonCommandHandlerImpl implements LinkUserToPersonCommandHandler {

  private final UserPersonLinkRepository userPersonLinkRepository;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public LinkUserToPersonResult handle(LinkUserToPersonCommand command) {
    if (userPersonLinkRepository.existsByPersonId(command.personId())) {
      throw new DomainValidationException("Esa persona ya está vinculada a un usuario");
    }
    if (userPersonLinkRepository.existsByUserIdAndChurchId(command.userId(), command.churchId())) {
      throw new DomainValidationException("Ese usuario ya tiene una persona en esta iglesia");
    }
    userPersonLinkRepository.save(
        new UserPersonLink(
            entityManager.getReference(AppUser.class, command.userId()),
            entityManager.getReference(Party.class, command.personId()),
            entityManager.getReference(Organization.class, command.churchId())));
    return new LinkUserToPersonResult();
  }
}
