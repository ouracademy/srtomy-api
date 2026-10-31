package org.ouracademy.ovejafiel.user.command.link_user_to_person;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.Party;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.ouracademy.ovejafiel.user.domain.UserPersonLink;
import org.ouracademy.ovejafiel.user.repository.UserPersonLinkRepository;
import org.ouracademy.ovejafiel.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class LinkUserToPersonCommandHandlerImpl implements LinkUserToPersonCommandHandler {

  private final UserPersonLinkRepository userPersonLinkRepository;
  private final UserRepository userRepository;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public LinkUserToPersonResult handle(LinkUserToPersonCommand command) {
    var current = userPersonLinkRepository.findByPersonId(command.personId());
    if (current.isPresent()) {
      var linked = current.get().getUser();
      if (!linked.isWhatsAppIdentity()) {
        throw new DomainValidationException("Esa persona ya está vinculada a un usuario");
      }
      // The user made for WhatsApp steps aside for a real account.
      userPersonLinkRepository.delete(current.get());
      userPersonLinkRepository.flush();
      userRepository.delete(linked);
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
