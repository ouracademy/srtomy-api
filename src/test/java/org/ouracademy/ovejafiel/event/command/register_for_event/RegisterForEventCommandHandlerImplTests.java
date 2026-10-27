package org.ouracademy.ovejafiel.event.command.register_for_event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.registration;
import static org.ouracademy.ovejafiel.event.EventFixtures.registrationSetup;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.event.EventFixtures.ticket;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationSetup;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.domain.TicketType;
import org.ouracademy.ovejafiel.event.domain.TicketTypeId;
import org.ouracademy.ovejafiel.event.query.EventAccess;
import org.ouracademy.ovejafiel.event.query.Seats;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.TicketTypeRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.springframework.security.access.AccessDeniedException;

class RegisterForEventCommandHandlerImplTests {

  EventRepository eventRepository = mock(EventRepository.class);
  EventRegistrationSetupRepository setups = mock(EventRegistrationSetupRepository.class);
  TicketTypeRepository tickets = mock(TicketTypeRepository.class);
  EventRegistrationRepository registrations = mock(EventRegistrationRepository.class);
  EventAccess eventAccess = mock(EventAccess.class);
  Seats seats = mock(Seats.class);
  EntityManager entityManager = mock(EntityManager.class);
  RegisterForEventCommandHandlerImpl handler =
      new RegisterForEventCommandHandlerImpl(
          eventRepository, setups, tickets, registrations, eventAccess, seats, entityManager);

  Event event =
      singleEvent(
          EventId.generate(),
          "Curso",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());
  EventRegistrationSetup setup = registrationSetup(event, false, null);
  TicketType general = ticket(event, "General", 10);

  @BeforeEach
  void setUp() {
    when(eventRepository.findByShareSlug("slug")).thenReturn(Optional.of(event));
    when(eventAccess.canOpen(any(), any())).thenReturn(true);
    when(setups.lockByEventId(event.getId())).thenReturn(Optional.of(setup));
    when(tickets.findById(general.getId())).thenReturn(Optional.of(general));
    when(registrations.findFirstByEventIdAndEmailIgnoreCaseAndStatusNot(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(seats.confirmedByTicket(event.getId())).thenReturn(new HashMap<>());
    when(seats.hasRoom(any(), any(), any())).thenReturn(true);
    when(registrations.save(any(EventRegistration.class))).thenAnswer(call -> call.getArgument(0));
  }

  RegisterForEventCommand command(String email) {
    return new RegisterForEventCommand(
        "slug", null, general.getId(), " Lucía ", "Mora ", email, " 999 ");
  }

  @Test
  void savesTheSignUpWithCleanedValuesAndASeat() {
    var result = handler.handle(command(" Lucia@X.org "));

    assertThat(result.alreadyRegistered()).isFalse();
    assertThat(result.status()).isEqualTo(RegistrationStatus.CONFIRMED);
    assertThat(result.cancelCode()).isNotBlank().isNotEqualTo(result.ticketCode());
    var saved = ArgumentCaptor.forClass(EventRegistration.class);
    verify(registrations).save(saved.capture());
    assertThat(saved.getValue().getFirstName()).isEqualTo("Lucía");
    assertThat(saved.getValue().getLastName()).isEqualTo("Mora");
    assertThat(saved.getValue().getEmail()).isEqualTo("lucia@x.org");
    assertThat(saved.getValue().getPhoneNumber()).isEqualTo("999");
    assertThat(saved.getValue().getTicketType()).isSameAs(general);
    assertThat(saved.getValue().getUser()).isNull();
  }

  @Test
  void aSignedInUserIsLinkedToTheirSignUp() {
    var user = new UserResult(UserId.generate(), "me", "lucia@x.org", true);
    when(eventAccess.verifiedUser("me")).thenReturn(user);
    var reference = new AppUser("me");
    when(entityManager.getReference(AppUser.class, user.id())).thenReturn(reference);

    handler.handle(
        new RegisterForEventCommand(
            "slug", "me", general.getId(), "Lu", "Mora", "lucia@x.org", null));

    var saved = ArgumentCaptor.forClass(EventRegistration.class);
    verify(registrations).save(saved.capture());
    assertThat(saved.getValue().getUser()).isSameAs(reference);
    assertThat(saved.getValue().getPhoneNumber()).isNull();
  }

  @Test
  void theSameEmailTwiceDoesNotAddAnotherAndReturnsTheCodesOfTheFirst() {
    var first = registration(event, general, "lucia@x.org", RegistrationStatus.WAITLISTED);
    when(registrations.findFirstByEventIdAndEmailIgnoreCaseAndStatusNot(
            event.getId(), "lucia@x.org", RegistrationStatus.CANCELLED))
        .thenReturn(Optional.of(first));

    var result = handler.handle(command("lucia@x.org"));

    assertThat(result.alreadyRegistered()).isTrue();
    assertThat(result.status()).isEqualTo(RegistrationStatus.WAITLISTED);
    assertThat(result.cancelCode()).isEqualTo(first.getCancelCode());
    assertThat(result.ticketCode()).isEqualTo(first.getTicketCode());
    verify(registrations, never()).save(any());
  }

  @Test
  void aFullTicketWithoutWaitlistIsRefused() {
    when(seats.hasRoom(any(), any(), any())).thenReturn(false);

    assertThatThrownBy(() -> handler.handle(command("lucia@x.org")))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("agotaron");
    verify(registrations, never()).save(any());
  }

  @Test
  void aFullTicketWithWaitlistPutsThemOnTheWaitlist() {
    setup.configure(true, false, true, null);
    when(seats.hasRoom(any(), any(), any())).thenReturn(false);

    var result = handler.handle(command("lucia@x.org"));

    assertThat(result.status()).isEqualTo(RegistrationStatus.WAITLISTED);
  }

  @Test
  void aClosedRegistrationIsRefused() {
    setup.configure(false, false, false, null);

    assertThatThrownBy(() -> handler.handle(command("lucia@x.org")))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("cerrada");
  }

  @Test
  void anEventWithoutRegistrationIsRefused() {
    when(setups.lockByEventId(event.getId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(command("lucia@x.org")))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void aRegistrationThatNeedsAnAccountRefusesAnonymousVisitors() {
    setup.configure(true, true, false, null);

    assertThatThrownBy(() -> handler.handle(command("lucia@x.org")))
        .isInstanceOf(AccessDeniedException.class);
    verify(registrations, never()).save(any());
  }

  @Test
  void whoCannotOpenTheEventCannotSignUp() {
    when(eventAccess.canOpen(any(), any())).thenReturn(false);

    assertThatThrownBy(() -> handler.handle(command("lucia@x.org")))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void aTicketOfAnotherEventIsRefused() {
    var other =
        singleEvent(
            EventId.generate(),
            "Otro",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            List.of());
    var foreign = ticket(other, "Ajeno", null);
    when(tickets.findById(foreign.getId())).thenReturn(Optional.of(foreign));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new RegisterForEventCommand(
                        "slug", null, foreign.getId(), "Lu", "Mora", "lu@x.org", null)))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void anUnknownTicketIsRefused() {
    assertThatThrownBy(
            () ->
                handler.handle(
                    new RegisterForEventCommand(
                        "slug", null, TicketTypeId.generate(), "Lu", "Mora", "lu@x.org", null)))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void aTicketOutsideItsSalesWindowIsRefused() {
    general.describe("General", 0, "PEN", 10, null, ZonedDateTime.now().minusDays(1));

    assertThatThrownBy(() -> handler.handle(command("lucia@x.org")))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("venta");
  }

  @Test
  void theNamesAndTheEmailAreRequired() {
    assertThatThrownBy(
            () ->
                handler.handle(
                    new RegisterForEventCommand(
                        "slug", null, general.getId(), " ", "Mora", "lu@x.org", null)))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(
            () ->
                handler.handle(
                    new RegisterForEventCommand(
                        "slug", null, general.getId(), "Lu", null, "lu@x.org", null)))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> handler.handle(command("not-an-email")))
        .isInstanceOf(DomainValidationException.class);
    verify(registrations, never()).save(any());
  }

  @Test
  void anUnknownLinkIsNotFound() {
    when(eventRepository.findByShareSlug("nope")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                handler.handle(
                    new RegisterForEventCommand(
                        "nope", null, general.getId(), "Lu", "Mora", "lu@x.org", null)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
