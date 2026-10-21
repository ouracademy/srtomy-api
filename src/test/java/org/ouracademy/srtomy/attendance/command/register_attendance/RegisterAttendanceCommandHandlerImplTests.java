package org.ouracademy.srtomy.attendance.command.register_attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.attendance.AttendanceFixtures.eventAttendance;
import static org.ouracademy.srtomy.event.EventFixtures.singleEvent;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;
import static org.ouracademy.srtomy.person.PersonFixtures.person;
import static org.ouracademy.srtomy.person.PersonFixtures.personResult;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.attendance.domain.EventAttendance;
import org.ouracademy.srtomy.attendance.repository.EventAttendanceRepository;
import org.ouracademy.srtomy.event.domain.Event;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.SingleEvent;
import org.ouracademy.srtomy.event.query.FindEventResult;
import org.ouracademy.srtomy.event.query.find_event_by_id.FindEventByIdQuery;
import org.ouracademy.srtomy.event.query.find_event_by_id.FindEventByIdQueryHandler;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.domain.Person;
import org.ouracademy.srtomy.person.query.find_people_by_ids.FindPeopleByIdsQuery;
import org.ouracademy.srtomy.person.query.find_people_by_ids.FindPeopleByIdsQueryHandler;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegisterAttendanceCommandHandlerImplTests {

  @Mock EventAttendanceRepository eventAttendanceRepository;
  @Mock FindEventByIdQueryHandler findEventByIdQueryHandler;
  @Mock FindPeopleByIdsQueryHandler findPeopleByIdsQueryHandler;
  @Mock EntityManager entityManager;

  RegisterAttendanceCommandHandlerImpl handler;

  EventId eventId = EventId.generate();
  PartyId personId = PartyId.generate();
  Person attendee = person(personId, "Jose", "Mena");
  SingleEvent event =
      singleEvent(
          eventId,
          "Culto",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @BeforeEach
  void setUp() {
    handler =
        new RegisterAttendanceCommandHandlerImpl(
            eventAttendanceRepository,
            findEventByIdQueryHandler,
            findPeopleByIdsQueryHandler,
            entityManager);
  }

  private void eventAndAttendeeExist() {
    when(findEventByIdQueryHandler.handle(new FindEventByIdQuery(eventId)))
        .thenReturn(Optional.of(FindEventResult.from(event)));
    when(findPeopleByIdsQueryHandler.handle(new FindPeopleByIdsQuery(List.of(personId))))
        .thenReturn(List.of(personResult(personId, "Jose", "Mena")));
    when(entityManager.getReference(Person.class, personId)).thenReturn(attendee);
  }

  @Test
  void handleCreatesTheAttendanceReferencingTheEventAndAttendees() {
    eventAndAttendeeExist();
    when(eventAttendanceRepository.findByEventId(eventId)).thenReturn(Optional.empty());
    when(entityManager.getReference(Event.class, eventId)).thenReturn(event);
    // Mimics persist assigning the @MapsId id from the event.
    when(eventAttendanceRepository.save(any(EventAttendance.class)))
        .thenAnswer(
            call -> {
              EventAttendance attendance = call.getArgument(0);
              ReflectionTestUtils.setField(attendance, "id", attendance.getEvent().getId());
              return attendance;
            });

    var result = handler.handle(new RegisterAttendanceCommand(eventId, List.of(personId)));

    assertThat(result).isEqualTo(new RegisterAttendanceResult(eventId, List.of(personId)));
    var saved = ArgumentCaptor.forClass(EventAttendance.class);
    verify(eventAttendanceRepository, times(2)).save(saved.capture());
    var attendance = saved.getValue();
    assertThat(attendance.getEvent()).isSameAs(event);
    assertThat(attendance.getDateTime()).isNotNull();
    assertThat(attendance.getPersonIds()).containsExactly(personId);
    assertThat(attendance.getPersonEventAttendances())
        .singleElement()
        .satisfies(p -> assertThat(p.getPerson()).isSameAs(attendee));
  }

  @Test
  void handleReplacesTheAttendeesOfAnExistingAttendance() {
    eventAndAttendeeExist();
    var previousAttendee = person(PartyId.generate(), "Ana", "Rojas");
    var existing = eventAttendance(event, List.of(previousAttendee));
    when(eventAttendanceRepository.findByEventId(eventId)).thenReturn(Optional.of(existing));
    when(eventAttendanceRepository.save(existing)).thenReturn(existing);

    handler.handle(new RegisterAttendanceCommand(eventId, List.of(personId)));

    verify(entityManager, never()).getReference(Event.class, eventId);
    verify(eventAttendanceRepository, times(2)).save(existing);
    assertThat(existing.getPersonIds()).containsExactly(personId);
  }

  @Test
  void handleFailsWhenTheEventDoesNotExist() {
    when(findEventByIdQueryHandler.handle(new FindEventByIdQuery(eventId)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> handler.handle(new RegisterAttendanceCommand(eventId, List.of(personId))))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining(eventId.value().toString());

    verifyNoInteractions(eventAttendanceRepository, entityManager, findPeopleByIdsQueryHandler);
  }

  @Test
  void handleFailsWhenAnAttendeeDoesNotExist() {
    var missingPersonId = PartyId.generate();
    when(findEventByIdQueryHandler.handle(new FindEventByIdQuery(eventId)))
        .thenReturn(Optional.of(FindEventResult.from(event)));
    when(findPeopleByIdsQueryHandler.handle(
            new FindPeopleByIdsQuery(List.of(personId, missingPersonId))))
        .thenReturn(List.of(personResult(personId, "Jose", "Mena")));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new RegisterAttendanceCommand(eventId, List.of(personId, missingPersonId))))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining(missingPersonId.value().toString())
        .hasMessageNotContaining(personId.value().toString());

    verifyNoInteractions(eventAttendanceRepository, entityManager);
  }
}
