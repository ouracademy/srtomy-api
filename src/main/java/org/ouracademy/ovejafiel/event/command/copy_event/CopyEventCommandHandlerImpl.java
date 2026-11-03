package org.ouracademy.ovejafiel.event.command.copy_event;

import java.time.Duration;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The congress of next year starts as a copy of this one. Only what describes the events is copied
 * (name, place, speakers, kind and, for the program, how each sub-event is entered): no sign-ups,
 * no attendance, no sharing (it starts private so it can be reviewed first) and no registration
 * setup.
 */
@Service
@RequiredArgsConstructor
class CopyEventCommandHandlerImpl implements CopyEventCommandHandler {

  private final EventRepository eventRepository;

  @Override
  @Transactional
  public CopyEventResult handle(CopyEventCommand command) {
    var original =
        eventRepository
            .findById(command.eventId())
            .orElseThrow(() -> new NoSuchElementException("Event not found"));
    if (original.isChild()) {
      throw new DomainValidationException(
          "Un sub-evento no se copia solo: copie el evento al que pertenece");
    }
    if (original.getSeries() != null) {
      throw new DomainValidationException("Un evento que se repite no se puede copiar");
    }
    var shift = Duration.between(original.getStart().toInstant(), command.newStart().toInstant());
    var name =
        command.name() == null || command.name().isBlank() ? original.getTitle() : command.name();

    var copy = copyOf(original, name, shift);
    eventRepository.save(copy);
    var subEvents = 0;
    for (var child : eventRepository.findByParentOrderByStart(original)) {
      var copiedChild = copyOf(child, child.getTitle(), shift);
      copiedChild.attachTo(copy);
      copiedChild.setRegistrationMode(child.getRegistrationMode());
      eventRepository.save(copiedChild);
      subEvents++;
    }
    return new CopyEventResult(copy.getId(), subEvents);
  }

  private Event copyOf(Event event, String name, Duration shift) {
    var copy = new Event(name, event.getStart().plus(shift), event.getEnd().plus(shift));
    copy.setOrganization(event.getOrganization());
    copy.setAddress(event.getAddress());
    copy.setDescription(event.getDescription());
    copy.setIsACourse(event.getIsACourse());
    copy.setSpeakers(new ArrayList<>(event.getSpeakers()));
    return copy;
  }
}
