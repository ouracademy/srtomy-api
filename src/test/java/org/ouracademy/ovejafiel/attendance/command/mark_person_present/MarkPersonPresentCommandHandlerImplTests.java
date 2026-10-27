package org.ouracademy.ovejafiel.attendance.command.mark_person_present;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.attendance.AttendanceFixtures.eventAttendance;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;
import static org.ouracademy.ovejafiel.person.PersonFixtures.person;
import static org.ouracademy.ovejafiel.person.PersonFixtures.personResult;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.attendance.repository.EventAttendanceRepository;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.FindEventResult;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQuery;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQueryHandler;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQuery;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQueryHandler;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.ouracademy.ovejafiel.user.domain.UserId;

class MarkPersonPresentCommandHandlerImplTests {

  EventAttendanceRepository attendances = mock(EventAttendanceRepository.class);
  FindEventByIdQueryHandler events = mock(FindEventByIdQueryHandler.class);
  FindPeopleByIdsQueryHandler people = mock(FindPeopleByIdsQueryHandler.class);
  EntityManager entityManager = mock(EntityManager.class);
  MarkPersonPresentCommandHandlerImpl handler =
      new MarkPersonPresentCommandHandlerImpl(attendances, events, people, entityManager);

  EventId eventId = EventId.generate();
  PartyId personId = PartyId.generate();
  Person ana = person(personId, "Ana", "Paz");
  Event event =
      singleEvent(
          eventId,
          "Culto",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @BeforeEach
  void setUp() {
    when(events.handle(new FindEventByIdQuery(eventId)))
        .thenReturn(Optional.of(FindEventResult.from(event)));
    when(people.handle(new FindPeopleByIdsQuery(List.of(personId))))
        .thenReturn(List.of(personResult(personId, "Ana", "Paz")));
    when(entityManager.getReference(Person.class, personId)).thenReturn(ana);
  }

  @Test
  void marksThePersonWithHowAndByWhom() {
    var attendance = eventAttendance(event, List.of());
    when(attendances.findByEventId(eventId)).thenReturn(Optional.of(attendance));
    var usherId = UserId.generate();
    var usher = new AppUser("usher");
    when(entityManager.getReference(AppUser.class, usherId)).thenReturn(usher);

    var result =
        handler.handle(
            new MarkPersonPresentCommand(eventId, personId, CheckInSource.QR_TICKET, usherId));

    assertThat(result.alreadyMarked()).isFalse();
    verify(attendances).createIfMissing(eventId.value());
    verify(attendances).save(attendance);
    var mark = attendance.getPersonEventAttendances().get(0);
    assertThat(mark.getSource()).isEqualTo(CheckInSource.QR_TICKET);
    assertThat(mark.getMarkedBy()).isSameAs(usher);
  }

  @Test
  void someoneMarkingThemselvesHasNoMarker() {
    var attendance = eventAttendance(event, List.of());
    when(attendances.findByEventId(eventId)).thenReturn(Optional.of(attendance));

    handler.handle(new MarkPersonPresentCommand(eventId, personId, CheckInSource.QR_EVENT, null));

    assertThat(attendance.getPersonEventAttendances().get(0).getMarkedBy()).isNull();
  }

  @Test
  void markingAPersonAlreadyThereChangesNothing() {
    var attendance = eventAttendance(event, List.of(ana));
    when(attendances.findByEventId(eventId)).thenReturn(Optional.of(attendance));

    var result =
        handler.handle(
            new MarkPersonPresentCommand(eventId, personId, CheckInSource.QR_EVENT, null));

    assertThat(result.alreadyMarked()).isTrue();
    assertThat(attendance.getPersonEventAttendances()).hasSize(1);
    assertThat(attendance.getPersonEventAttendances().get(0).getSource())
        .isEqualTo(CheckInSource.MANUAL);
  }

  @Test
  void anUnknownEventOrPersonIsNotFound() {
    var otherEvent = EventId.generate();
    when(events.handle(new FindEventByIdQuery(otherEvent))).thenReturn(Optional.empty());
    var stranger = PartyId.generate();
    when(people.handle(new FindPeopleByIdsQuery(List.of(stranger)))).thenReturn(List.of());

    assertThatThrownBy(
            () ->
                handler.handle(
                    new MarkPersonPresentCommand(otherEvent, personId, CheckInSource.MANUAL, null)))
        .isInstanceOf(NoSuchElementException.class);
    assertThatThrownBy(
            () ->
                handler.handle(
                    new MarkPersonPresentCommand(eventId, stranger, CheckInSource.MANUAL, null)))
        .isInstanceOf(NoSuchElementException.class);
    verify(attendances, never()).save(any());
  }
}
