package org.ouracademy.ovejafiel.event.query;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.event.domain.RegistrationMode;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.domain.SubEventCheckIn;
import org.ouracademy.ovejafiel.event.repository.SubEventCheckInRepository;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.stereotype.Component;

/**
 * The entry into a sub-event that {@link RegistrationMode#INHERIT inherits} the sign-up of its mega
 * event. The sign-up belongs to the mega event, so its own check-in is not touched: each sub-event
 * keeps its own record of who came in.
 */
@Component
@RequiredArgsConstructor
public class InheritedCheckIns {

  private final SubEventCheckInRepository repository;

  /** The sub-event shares the sign-up of its mega event. */
  public boolean inherits(Event event) {
    return event.isChild() && event.getRegistrationMode() == RegistrationMode.INHERIT;
  }

  /** The sign-up is the mega event's and {@code subEvent} inherits it. */
  public boolean covers(Event subEvent, EventRegistration registration) {
    return inherits(subEvent)
        && subEvent.getParent().getId().equals(registration.getEvent().getId());
  }

  /** What happened when they were marked: {@code already} when it had been done before. */
  public record Mark(boolean already, LocalDateTime checkedInAt) {}

  /** Marks them as in; doing it again keeps the first time. */
  public Mark mark(EventRegistration registration, Event subEvent, CheckInSource source) {
    if (registration.getStatus() != RegistrationStatus.CONFIRMED) {
      throw new DomainValidationException("Solo una inscripción confirmada puede ingresar");
    }
    var existing =
        repository.findByRegistrationIdAndEventId(registration.getId(), subEvent.getId());
    if (existing.isPresent()) {
      return new Mark(true, existing.get().getCheckedInAt());
    }
    var saved = repository.save(new SubEventCheckIn(registration, subEvent, source));
    return new Mark(false, saved.getCheckedInAt());
  }

  /** Takes the mark back, if there was one. */
  public void undo(EventRegistration registration, Event subEvent) {
    repository
        .findByRegistrationIdAndEventId(registration.getId(), subEvent.getId())
        .ifPresent(repository::delete);
  }

  /** Who came into {@code subEvent}, by sign-up. */
  public Map<EventRegistrationId, SubEventCheckIn> of(Event subEvent) {
    return repository.findByEventId(subEvent.getId()).stream()
        .collect(Collectors.toMap(found -> found.getRegistration().getId(), Function.identity()));
  }
}
