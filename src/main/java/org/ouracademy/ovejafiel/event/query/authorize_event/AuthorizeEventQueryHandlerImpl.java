package org.ouracademy.ovejafiel.event.query.authorize_event;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.query.EventAccess;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class AuthorizeEventQueryHandlerImpl implements AuthorizeEventQueryHandler {

  private final EventRepository eventRepository;
  private final EventAccess eventAccess;

  @Override
  @Transactional(readOnly = true)
  public AuthorizeEventResult handle(AuthorizeEventQuery query) {
    var event =
        eventRepository
            .findById(query.eventId())
            .orElseThrow(
                () -> new NoSuchElementException("Event not found: " + query.eventId().value()));
    var level = eventAccess.levelOn(query.subject(), event);
    if (!level.atLeast(query.required())) {
      throw new AccessDeniedException("No tienes permiso para realizar esta acción");
    }
    return new AuthorizeEventResult(level);
  }
}
