package org.ouracademy.srtomy.event.command.register_for_event;

import jakarta.persistence.EntityManager;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.event.domain.EventRegistration;
import org.ouracademy.srtomy.event.query.EventAccess;
import org.ouracademy.srtomy.event.repository.EventRegistrationRepository;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.ouracademy.srtomy.user.domain.AppUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class RegisterForEventCommandHandlerImpl implements RegisterForEventCommandHandler {

  private final EventRepository eventRepository;
  private final EventRegistrationRepository eventRegistrationRepository;
  private final EventAccess eventAccess;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public RegisterForEventResult handle(RegisterForEventCommand command) {
    var event =
        eventRepository
            .findByShareSlug(command.slug())
            .orElseThrow(() -> new NoSuchElementException("Event not found"));
    if (!eventAccess.canRegister(command.subject(), event)) {
      throw new AccessDeniedException("La inscripción por este enlace no está abierta");
    }
    var firstName = required(command.firstName(), "Indique su nombre");
    var lastName = required(command.lastName(), "Indique su apellido");
    var email = command.email() == null ? "" : command.email().trim().toLowerCase();
    if (!email.contains("@")) {
      throw new DomainValidationException("El correo no es válido");
    }
    if (eventRegistrationRepository.existsByEventIdAndEmailIgnoreCase(event.getId(), email)) {
      return new RegisterForEventResult(true);
    }
    var user = eventAccess.verifiedUser(command.subject());
    eventRegistrationRepository.save(
        new EventRegistration(
            event,
            user == null ? null : entityManager.getReference(AppUser.class, user.id()),
            firstName,
            lastName,
            email,
            blankToNull(command.phoneNumber())));
    return new RegisterForEventResult(false);
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
