package org.ouracademy.ovejafiel.event.query;

import java.util.Optional;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.RegistrationMode;
import org.springframework.stereotype.Component;

/**
 * Which event's registration rules who can enter an event. An event on its own, or a sub-event with
 * {@link RegistrationMode#OWN}, answers to itself; a sub-event that {@link RegistrationMode#INHERIT
 * inherits} answers to its container; an {@link RegistrationMode#OPEN} one answers to nobody.
 */
@Component
public class EffectiveRegistration {

  public Optional<Event> governing(Event event) {
    if (!event.isChild()) {
      return Optional.of(event);
    }
    return switch (event.getRegistrationMode()) {
      case OWN -> Optional.of(event);
      case INHERIT -> Optional.of(event.getParent());
      case OPEN -> Optional.empty();
    };
  }

  /**
   * Whether the registration of {@code event} itself (its setup and tickets) is the one in force.
   */
  public boolean isOwn(Event event) {
    return governing(event).filter(found -> found.getId().equals(event.getId())).isPresent();
  }
}
