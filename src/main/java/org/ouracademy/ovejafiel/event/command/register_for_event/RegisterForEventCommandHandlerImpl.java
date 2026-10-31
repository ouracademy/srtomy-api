package org.ouracademy.ovejafiel.event.command.register_for_event;

import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.domain.RegistrationMode;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.EffectiveRegistration;
import org.ouracademy.ovejafiel.event.query.EventAccess;
import org.ouracademy.ovejafiel.event.query.Seats;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.TicketTypeRepository;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class RegisterForEventCommandHandlerImpl implements RegisterForEventCommandHandler {

  private final EventRepository eventRepository;
  private final EventRegistrationSetupRepository setupRepository;
  private final TicketTypeRepository ticketTypeRepository;
  private final EventRegistrationRepository eventRegistrationRepository;
  private final EventAccess eventAccess;
  private final Seats seats;
  private final EffectiveRegistration effectiveRegistration;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public RegisterForEventResult handle(RegisterForEventCommand command) {
    var event =
        eventRepository
            .findByShareSlug(command.slug())
            .orElseThrow(() -> new NoSuchElementException("Event not found"));
    if (!eventAccess.canOpen(command.subject(), event)) {
      throw new AccessDeniedException("No puede inscribirse en este evento");
    }
    rejectIfRegistrationIsNotItsOwn(event);
    // The lock keeps two people from taking the last seat at the same time.
    var setup =
        setupRepository
            .lockByEventId(event.getId())
            .orElseThrow(() -> new DomainValidationException("Este evento no tiene inscripción"));
    if (!setup.isOpen()) {
      throw new DomainValidationException("La inscripción está cerrada");
    }
    var user = eventAccess.verifiedUser(command.subject());
    if (setup.isRequiresAccount() && user == null) {
      throw new AccessDeniedException("Inicie sesión con un correo verificado para inscribirse");
    }

    var firstName = required(command.firstName(), "Indique su nombre");
    var lastName = required(command.lastName(), "Indique su apellido");
    var email = command.email() == null ? "" : command.email().trim().toLowerCase();
    if (!email.contains("@")) {
      throw new DomainValidationException("El correo no es válido");
    }

    var existing =
        eventRegistrationRepository.findFirstByEventIdAndEmailIgnoreCaseAndStatusNot(
            event.getId(), email, RegistrationStatus.CANCELLED);
    if (existing.isPresent()) {
      var found = existing.get();
      return new RegisterForEventResult(
          true, found.getStatus(), found.getCancelCode(), found.getTicketCode());
    }

    var ticket =
        ticketTypeRepository
            .findById(command.ticketTypeId())
            .filter(found -> found.getEvent().getId().equals(event.getId()))
            .orElseThrow(() -> new DomainValidationException("Elija un ticket"));
    if (!ticket.isOnSale(ZonedDateTime.now())) {
      throw new DomainValidationException("La venta de este ticket no está abierta");
    }

    if (!seats.hasRoom(setup, ticket, seats.confirmedByTicket(event.getId()))) {
      throw new DomainValidationException("Se agotaron los lugares de este ticket");
    }

    var saved =
        eventRegistrationRepository.save(
            new EventRegistration(
                event,
                ticket,
                user == null ? null : entityManager.getReference(AppUser.class, user.id()),
                firstName,
                lastName,
                email,
                blankToNull(command.phoneNumber()),
                RegistrationStatus.CONFIRMED));
    return new RegisterForEventResult(
        false, saved.getStatus(), saved.getCancelCode(), saved.getTicketCode());
  }

  /** A sub-event that inherits or is open has no registration of its own to sign up for. */
  private void rejectIfRegistrationIsNotItsOwn(Event event) {
    if (effectiveRegistration.isOwn(event)) {
      return;
    }
    if (event.getRegistrationMode() == RegistrationMode.OPEN) {
      throw new DomainValidationException("Este sub-evento no pide inscripción");
    }
    throw new DomainValidationException(
        "Este sub-evento usa la inscripción del evento «" + event.getParent().getTitle() + "»");
  }

  private static String required(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new DomainValidationException(message);
    }
    return value.trim();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
