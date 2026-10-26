package org.ouracademy.ovejafiel.attendance.command.register_attendance;

import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.attendance.domain.EventAttendance;
import org.ouracademy.ovejafiel.attendance.repository.EventAttendanceRepository;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQuery;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.person.query.FindPersonResult;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQuery;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The event and the attendees are validated through their modules' query handlers; the JPA
 * references for {@code EventAttendance.event} and the attendees are then plain {@link
 * EntityManager#getReference} calls so this module never touches their repositories.
 */
@Service
@RequiredArgsConstructor
class RegisterAttendanceCommandHandlerImpl implements RegisterAttendanceCommandHandler {

  private final EventAttendanceRepository eventAttendanceRepository;
  private final FindEventByIdQueryHandler findEventByIdQueryHandler;
  private final FindPeopleByIdsQueryHandler findPeopleByIdsQueryHandler;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public RegisterAttendanceResult handle(RegisterAttendanceCommand command) {
    var eventId = command.eventId();
    validateEventExists(eventId);
    validatePeopleExist(command.personIds());

    var eventAttendance =
        eventAttendanceRepository
            .findByEventId(eventId)
            .orElseGet(() -> new EventAttendance(entityManager.getReference(Event.class, eventId)));

    eventAttendance = eventAttendanceRepository.save(eventAttendance);
    eventAttendance.setPeople(
        command.personIds().stream()
            .map(personId -> entityManager.getReference(Person.class, personId))
            .toList());
    eventAttendanceRepository.save(eventAttendance);

    return new RegisterAttendanceResult(eventId, command.personIds());
  }

  private void validateEventExists(EventId eventId) {
    findEventByIdQueryHandler
        .handle(new FindEventByIdQuery(eventId))
        .orElseThrow(() -> new NoSuchElementException("Event not found: " + eventId.value()));
  }

  private void validatePeopleExist(List<PartyId> personIds) {
    var missing = new HashSet<>(personIds);
    findPeopleByIdsQueryHandler.handle(new FindPeopleByIdsQuery(personIds)).stream()
        .map(FindPersonResult::id)
        .forEach(missing::remove);
    if (!missing.isEmpty()) {
      throw new NoSuchElementException(
          "People not found: " + missing.stream().map(PartyId::value).toList());
    }
  }
}
