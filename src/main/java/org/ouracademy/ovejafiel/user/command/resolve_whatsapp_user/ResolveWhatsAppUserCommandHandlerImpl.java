package org.ouracademy.ovejafiel.user.command.resolve_whatsapp_user;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.Party;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.ouracademy.ovejafiel.user.domain.UserPersonLink;
import org.ouracademy.ovejafiel.user.repository.UserPersonLinkRepository;
import org.ouracademy.ovejafiel.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ResolveWhatsAppUserCommandHandlerImpl implements ResolveWhatsAppUserCommandHandler {

  private final UserPersonLinkRepository links;
  private final UserRepository users;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public ResolveWhatsAppUserResult handle(ResolveWhatsAppUserCommand command) {
    var existing = links.findByPersonId(command.personId());
    if (existing.isPresent()) {
      var user = existing.get().getUser();
      return new ResolveWhatsAppUserResult(user.getSubject(), !user.isWhatsAppIdentity());
    }
    var shadow = users.save(AppUser.forWhatsApp(command.personId()));
    links.save(
        new UserPersonLink(
            shadow,
            entityManager.getReference(Party.class, command.personId()),
            entityManager.getReference(Organization.class, command.churchId())));
    return new ResolveWhatsAppUserResult(shadow.getSubject(), false);
  }
}
