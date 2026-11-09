package org.ouracademy.ovejafiel.user.command.resolve_whatsapp_user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.Party;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.ouracademy.ovejafiel.user.domain.UserPersonLink;
import org.ouracademy.ovejafiel.user.repository.UserPersonLinkRepository;
import org.ouracademy.ovejafiel.user.repository.UserRepository;

class ResolveWhatsAppUserCommandHandlerImplTests {

  UserPersonLinkRepository links = mock(UserPersonLinkRepository.class);
  UserRepository users = mock(UserRepository.class);
  EntityManager entityManager = mock(EntityManager.class);
  ResolveWhatsAppUserCommandHandlerImpl handler =
      new ResolveWhatsAppUserCommandHandlerImpl(links, users, entityManager);

  PartyId person = PartyId.generate();
  PartyId church = PartyId.generate();

  @Test
  void aPersonWithoutUserGetsAShadowUserLinkedToTheirChurch() {
    when(links.findByPersonId(person)).thenReturn(Optional.empty());
    when(users.save(any(AppUser.class))).thenAnswer(call -> call.getArgument(0));
    when(entityManager.getReference(Party.class, person)).thenReturn(mock(Party.class));
    when(entityManager.getReference(Organization.class, church))
        .thenReturn(mock(Organization.class));

    var result = handler.handle(new ResolveWhatsAppUserCommand(person, church));

    assertThat(result.subject()).isEqualTo("whatsapp:" + person.value());
    assertThat(result.realAccount()).isFalse();
    var saved = ArgumentCaptor.forClass(AppUser.class);
    verify(users).save(saved.capture());
    assertThat(saved.getValue().isWhatsAppIdentity()).isTrue();
    verify(links).save(any(UserPersonLink.class));
  }

  @Test
  void theShadowUserIsReusedTheNextTime() {
    var link = mock(UserPersonLink.class);
    when(link.getUser()).thenReturn(AppUser.forWhatsApp(person));
    when(links.findByPersonId(person)).thenReturn(Optional.of(link));

    var result = handler.handle(new ResolveWhatsAppUserCommand(person, church));

    assertThat(result.subject()).isEqualTo("whatsapp:" + person.value());
    assertThat(result.realAccount()).isFalse();
    verify(users, never()).save(any());
    verify(links, never()).save(any());
  }

  @Test
  void aPersonWithARealAccountIsReportedAsSuchAndNothingIsCreated() {
    var link = mock(UserPersonLink.class);
    when(link.getUser()).thenReturn(new AppUser("auth0|ana"));
    when(links.findByPersonId(person)).thenReturn(Optional.of(link));

    var result = handler.handle(new ResolveWhatsAppUserCommand(person, church));

    assertThat(result.subject()).isEqualTo("auth0|ana");
    assertThat(result.realAccount()).isTrue();
    verify(users, never()).save(any());
  }

  @Test
  void aRealAccountCannotLookLikeAShadowOne() {
    assertThat(new AppUser("auth0|whatsapp:fake").isWhatsAppIdentity()).isFalse();
    assertThat(new AppUser("whatsapp:" + person.value()).isWhatsAppIdentity()).isTrue();
  }
}
