package org.ouracademy.ovejafiel.user.command.link_user_to_person;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.Party;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.domain.UserPersonLink;
import org.ouracademy.ovejafiel.user.repository.UserPersonLinkRepository;
import org.ouracademy.ovejafiel.user.repository.UserRepository;

class LinkUserToPersonCommandHandlerImplTests {

  UserPersonLinkRepository repository = mock(UserPersonLinkRepository.class);
  EntityManager entityManager = mock(EntityManager.class);
  UserRepository users = mock(UserRepository.class);
  LinkUserToPersonCommandHandlerImpl handler =
      new LinkUserToPersonCommandHandlerImpl(repository, users, entityManager);

  UserId userId = UserId.generate();
  PartyId personId = PartyId.generate();
  PartyId churchId = PartyId.generate();

  @Test
  void linksTheUserToThePersonOfTheChurch() {
    when(entityManager.getReference(AppUser.class, userId)).thenReturn(mock(AppUser.class));
    when(entityManager.getReference(Party.class, personId)).thenReturn(mock(Party.class));
    when(entityManager.getReference(Organization.class, churchId))
        .thenReturn(mock(Organization.class));

    handler.handle(new LinkUserToPersonCommand(userId, personId, churchId));

    verify(repository).save(any(UserPersonLink.class));
  }

  @Test
  void aPersonAlreadyLinkedToAUserIsRejected() {
    var link = mock(UserPersonLink.class);
    when(link.getUser()).thenReturn(new AppUser("auth0|someone"));
    when(repository.findByPersonId(personId)).thenReturn(Optional.of(link));

    assertThatThrownBy(
            () -> handler.handle(new LinkUserToPersonCommand(userId, personId, churchId)))
        .isInstanceOf(DomainValidationException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void aRealAccountReplacesTheUserMadeForWhatsApp() {
    var shadow = AppUser.forWhatsApp(personId);
    var link = mock(UserPersonLink.class);
    when(link.getUser()).thenReturn(shadow);
    when(repository.findByPersonId(personId)).thenReturn(Optional.of(link));
    when(entityManager.getReference(AppUser.class, userId)).thenReturn(mock(AppUser.class));
    when(entityManager.getReference(Party.class, personId)).thenReturn(mock(Party.class));
    when(entityManager.getReference(Organization.class, churchId))
        .thenReturn(mock(Organization.class));

    handler.handle(new LinkUserToPersonCommand(userId, personId, churchId));

    verify(repository).delete(link);
    verify(users).delete(shadow);
    verify(repository).save(any(UserPersonLink.class));
  }

  @Test
  void aUserWithAPersonInTheChurchAlreadyIsRejected() {
    when(repository.existsByUserIdAndChurchId(userId, churchId)).thenReturn(true);

    assertThatThrownBy(
            () -> handler.handle(new LinkUserToPersonCommand(userId, personId, churchId)))
        .isInstanceOf(DomainValidationException.class);
    verify(repository, never()).save(any());
  }
}
