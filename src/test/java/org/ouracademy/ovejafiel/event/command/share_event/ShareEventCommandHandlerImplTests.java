package org.ouracademy.ovejafiel.event.command.share_event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventShare;
import org.ouracademy.ovejafiel.event.domain.ShareRole;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.EventShareRepository;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommand;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommandHandler;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailResult;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.test.util.ReflectionTestUtils;

class ShareEventCommandHandlerImplTests {

  EventRepository eventRepository = mock(EventRepository.class);
  EventShareRepository shareRepository = mock(EventShareRepository.class);
  FindUserBySubjectQueryHandler findUser = mock(FindUserBySubjectQueryHandler.class);
  SendEmailCommandHandler sendEmail = mock(SendEmailCommandHandler.class);
  EntityManager entityManager = mock(EntityManager.class);
  ShareEventCommandHandlerImpl handler =
      new ShareEventCommandHandlerImpl(
          eventRepository, shareRepository, findUser, sendEmail, entityManager);

  EventId id = EventId.generate();
  Event event =
      singleEvent(
          id,
          "Retiro",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(handler, "appUrl", "https://app.example.org/");
    when(eventRepository.findById(id)).thenReturn(Optional.of(event));
    when(findUser.handle(any()))
        .thenReturn(Optional.of(new UserResult(UserId.generate(), "me", "pastor@x.org", true)));
    when(shareRepository.findByEventIdAndEmailIgnoreCase(any(), any()))
        .thenReturn(Optional.empty());
    when(shareRepository.save(any(EventShare.class))).thenAnswer(call -> call.getArgument(0));
    when(entityManager.getReference(eq(AppUser.class), any())).thenReturn(new AppUser("me"));
    when(sendEmail.handle(any())).thenReturn(new SendEmailResult(true));
  }

  @Test
  void sharesWithTheEmailAndTellsThemWithTheLink() {
    var result = handler.handle(new ShareEventCommand("me", id, " Ana@X.org ", ShareRole.VIEWER));

    var saved = ArgumentCaptor.forClass(EventShare.class);
    verify(shareRepository).save(saved.capture());
    assertThat(saved.getValue().getEmail()).isEqualTo("ana@x.org");
    assertThat(saved.getValue().getRole()).isEqualTo(ShareRole.VIEWER);
    assertThat(result.emailSent()).isTrue();
    assertThat(result.share().email()).isEqualTo("ana@x.org");

    var email = ArgumentCaptor.forClass(SendEmailCommand.class);
    verify(sendEmail).handle(email.capture());
    assertThat(email.getValue().to()).isEqualTo("ana@x.org");
    assertThat(email.getValue().subject()).contains("pastor@x.org", "Retiro");
    assertThat(email.getValue().text())
        .contains("https://app.example.org/e/" + event.getShareSlug());
  }

  @Test
  void sharingAgainChangesTheRoleAndResendsTheMessage() {
    var existing = new EventShare(event, "ana@x.org", ShareRole.VIEWER, null);
    when(shareRepository.findByEventIdAndEmailIgnoreCase(id, "ana@x.org"))
        .thenReturn(Optional.of(existing));

    handler.handle(new ShareEventCommand("me", id, "ana@x.org", ShareRole.EDITOR));

    assertThat(existing.getRole()).isEqualTo(ShareRole.EDITOR);
    verify(sendEmail).handle(any());
  }

  @Test
  void aFailedEmailKeepsTheShare() {
    when(sendEmail.handle(any())).thenReturn(new SendEmailResult(false));

    var result = handler.handle(new ShareEventCommand("me", id, "ana@x.org", ShareRole.VIEWER));

    assertThat(result.emailSent()).isFalse();
    verify(shareRepository).save(any(EventShare.class));
  }

  @Test
  void anInvalidEmailSharesNothing() {
    assertThatThrownBy(
            () -> handler.handle(new ShareEventCommand("me", id, "ana", ShareRole.VIEWER)))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(
            () -> handler.handle(new ShareEventCommand("me", id, null, ShareRole.VIEWER)))
        .isInstanceOf(DomainValidationException.class);
    verify(shareRepository, never()).save(any());
  }

  @Test
  void anUnknownEventIsNotFound() {
    when(eventRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> handler.handle(new ShareEventCommand("me", id, "a@x.org", ShareRole.VIEWER)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
