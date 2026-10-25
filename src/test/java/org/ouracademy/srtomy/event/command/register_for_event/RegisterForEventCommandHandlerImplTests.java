package org.ouracademy.srtomy.event.command.register_for_event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.event.EventFixtures.singleEvent;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.srtomy.event.domain.Event;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.EventRegistration;
import org.ouracademy.srtomy.event.query.EventAccess;
import org.ouracademy.srtomy.event.repository.EventRegistrationRepository;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.ouracademy.srtomy.user.domain.AppUser;
import org.ouracademy.srtomy.user.domain.UserId;
import org.ouracademy.srtomy.user.query.UserResult;
import org.springframework.security.access.AccessDeniedException;

class RegisterForEventCommandHandlerImplTests {

  EventRepository eventRepository = mock(EventRepository.class);
  EventRegistrationRepository registrations = mock(EventRegistrationRepository.class);
  EventAccess eventAccess = mock(EventAccess.class);
  EntityManager entityManager = mock(EntityManager.class);
  RegisterForEventCommandHandlerImpl handler =
      new RegisterForEventCommandHandlerImpl(
          eventRepository, registrations, eventAccess, entityManager);

  Event event =
      singleEvent(
          EventId.generate(),
          "Curso",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @BeforeEach
  void setUp() {
    when(eventRepository.findByShareSlug("slug")).thenReturn(Optional.of(event));
    when(eventAccess.canRegister(any(), any())).thenReturn(true);
    when(registrations.existsByEventIdAndEmailIgnoreCase(any(), any())).thenReturn(false);
  }

  RegisterForEventCommand command(String email) {
    return new RegisterForEventCommand("slug", null, " Lucía ", "Mora ", email, " 999 ");
  }

  @Test
  void savesTheSignUpWithCleanedValues() {
    var result = handler.handle(command(" Lucia@X.org "));

    assertThat(result.alreadyRegistered()).isFalse();
    var saved = ArgumentCaptor.forClass(EventRegistration.class);
    verify(registrations).save(saved.capture());
    assertThat(saved.getValue().getFirstName()).isEqualTo("Lucía");
    assertThat(saved.getValue().getLastName()).isEqualTo("Mora");
    assertThat(saved.getValue().getEmail()).isEqualTo("lucia@x.org");
    assertThat(saved.getValue().getPhoneNumber()).isEqualTo("999");
    assertThat(saved.getValue().getUser()).isNull();
  }

  @Test
  void aSignedInUserIsLinkedToTheirSignUp() {
    var user = new UserResult(UserId.generate(), "me", "lucia@x.org", true);
    when(eventAccess.verifiedUser("me")).thenReturn(user);
    var reference = new AppUser("me");
    when(entityManager.getReference(AppUser.class, user.id())).thenReturn(reference);

    handler.handle(new RegisterForEventCommand("slug", "me", "Lucía", "Mora", "lucia@x.org", null));

    var saved = ArgumentCaptor.forClass(EventRegistration.class);
    verify(registrations).save(saved.capture());
    assertThat(saved.getValue().getUser()).isSameAs(reference);
    assertThat(saved.getValue().getPhoneNumber()).isNull();
  }

  @Test
  void theSameEmailTwiceSavesNothingNew() {
    when(registrations.existsByEventIdAndEmailIgnoreCase(event.getId(), "lucia@x.org"))
        .thenReturn(true);

    assertThat(handler.handle(command("lucia@x.org")).alreadyRegistered()).isTrue();
    verify(registrations, never()).save(any());
  }

  @Test
  void aLinkThatDoesNotAllowSigningUpIsDenied() {
    when(eventAccess.canRegister(any(), any())).thenReturn(false);

    assertThatThrownBy(() -> handler.handle(command("lucia@x.org")))
        .isInstanceOf(AccessDeniedException.class);
    verify(registrations, never()).save(any());
  }

  @Test
  void anUnknownLinkIsNotFound() {
    when(eventRepository.findByShareSlug("slug")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(command("lucia@x.org")))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void aNameAndAValidEmailAreRequired() {
    assertThatThrownBy(
            () ->
                handler.handle(
                    new RegisterForEventCommand("slug", null, " ", "Mora", "a@x.org", null)))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("nombre");
    assertThatThrownBy(
            () ->
                handler.handle(
                    new RegisterForEventCommand("slug", null, "Lu", null, "a@x.org", null)))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> handler.handle(command("not-an-email")))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> handler.handle(command(null)))
        .isInstanceOf(DomainValidationException.class);
    verify(registrations, never()).save(any());
  }
}
