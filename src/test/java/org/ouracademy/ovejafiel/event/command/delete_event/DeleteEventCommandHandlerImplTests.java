package org.ouracademy.ovejafiel.event.command.delete_event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.childOf;
import static org.ouracademy.ovejafiel.event.EventFixtures.eventSeries;
import static org.ouracademy.ovejafiel.event.EventFixtures.occurrenceOf;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.attendance.command.delete_attendance_for_events.DeleteAttendanceForEventsCommand;
import org.ouracademy.ovejafiel.attendance.command.delete_attendance_for_events.DeleteAttendanceForEventsCommandHandler;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventChangeOption;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventSeries;
import org.ouracademy.ovejafiel.event.domain.Recurrence;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeleteEventCommandHandlerImplTests {

  @Mock EventRepository eventRepository;
  @Mock DeleteAttendanceForEventsCommandHandler deleteAttendanceForEventsCommandHandler;

  DeleteEventCommandHandlerImpl handler;

  EventSeries series;
  Event event;

  @BeforeEach
  void setUp() {
    handler =
        new DeleteEventCommandHandlerImpl(eventRepository, deleteAttendanceForEventsCommandHandler);
    var church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);
    series = eventSeries(church);
    event = occurrenceOf(series, EventId.generate());
  }

  private void givenTheEventExists() {
    when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
  }

  @Test
  void thisEventSoftDeletesOnlyThatEventAndItsAttendance() {
    givenTheEventExists();

    var result =
        handler.handle(new DeleteEventCommand(event.getId(), EventChangeOption.THIS_EVENT));

    assertThat(result.id()).isEqualTo(event.getId());
    assertThat(event.getDeletedTime()).isNotNull();
    assertThat(series.getDeletedTime()).isNull();
    assertThat(series.getRecurrence().getEndType()).isEqualTo(Recurrence.EndType.UNTIL);
    assertThat(series.getRecurrence().getUntil()).isEqualTo(LocalDate.of(2026, 1, 14));
    verify(deleteAttendanceForEventsCommandHandler)
        .handle(new DeleteAttendanceForEventsCommand(List.of(event.getId())));
  }

  @Test
  void thisAndTheFollowingEventsSoftDeletesThemAndEndsTheSeriesTheDayBefore() {
    givenTheEventExists();
    var following = occurrenceOf(series, EventId.generate());
    when(eventRepository.findBySeriesAndStartGreaterThanEqual(series, event.getStart()))
        .thenReturn(List.of(event, following));

    handler.handle(
        new DeleteEventCommand(event.getId(), EventChangeOption.THIS_AND_THE_FOLLOWING_EVENTS));

    assertThat(following.getDeletedTime()).isNotNull();
    assertThat(event.getDeletedTime()).isNotNull();
    // The event is on 2026-01-05, the first day: nothing is left of the series.
    assertThat(series.getDeletedTime()).isNotNull();
    verify(deleteAttendanceForEventsCommandHandler)
        .handle(new DeleteAttendanceForEventsCommand(List.of(event.getId(), following.getId())));
  }

  @Test
  void thisAndTheFollowingEventsKeepsTheEarlierOccurrences() {
    var later = occurrenceOf(series, EventId.generate());
    ReflectionTestUtils.setField(later, "start", ZonedDateTime.parse("2026-01-12T15:00:00Z[UTC]"));
    when(eventRepository.findById(later.getId())).thenReturn(Optional.of(later));
    when(eventRepository.findBySeriesAndStartGreaterThanEqual(series, later.getStart()))
        .thenReturn(List.of(later));

    handler.handle(
        new DeleteEventCommand(later.getId(), EventChangeOption.THIS_AND_THE_FOLLOWING_EVENTS));

    assertThat(series.getDeletedTime()).isNull();
    assertThat(series.getRecurrence().getUntil()).isEqualTo(LocalDate.of(2026, 1, 11));
    assertThat(series.isFullyMaterialized()).isTrue();
  }

  @Test
  void allEventsSoftDeletesEveryOccurrenceAndTheSeries() {
    givenTheEventExists();
    var other = occurrenceOf(series, EventId.generate());
    when(eventRepository.findBySeries(series)).thenReturn(List.of(event, other));

    handler.handle(new DeleteEventCommand(event.getId(), EventChangeOption.ALL_EVENTS));

    assertThat(event.getDeletedTime()).isNotNull();
    assertThat(other.getDeletedTime()).isNotNull();
    assertThat(series.getDeletedTime()).isNotNull();
    verify(deleteAttendanceForEventsCommandHandler)
        .handle(new DeleteAttendanceForEventsCommand(List.of(event.getId(), other.getId())));
  }

  @Test
  void aOneOffEventIsDeletedWhateverTheOption() {
    var oneOff = singleEvent(EventId.generate(), "Retiro", series.getOrganization(), List.of());
    when(eventRepository.findById(oneOff.getId())).thenReturn(Optional.of(oneOff));

    handler.handle(new DeleteEventCommand(oneOff.getId(), EventChangeOption.ALL_EVENTS));

    assertThat(oneOff.getDeletedTime()).isNotNull();
    verify(deleteAttendanceForEventsCommandHandler)
        .handle(new DeleteAttendanceForEventsCommand(List.of(oneOff.getId())));
  }

  @Test
  void deletingAMissingEventThrows() {
    var missingId = EventId.generate();
    when(eventRepository.findById(missingId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> handler.handle(new DeleteEventCommand(missingId, EventChangeOption.THIS_EVENT)))
        .isInstanceOf(NoSuchElementException.class);
    verifyNoInteractions(deleteAttendanceForEventsCommandHandler);
  }

  @Test
  void deletingAContainerDeletesItsSubEventsAndTheirAttendance() {
    var church = series.getOrganization();
    var conference = singleEvent(EventId.generate(), "Mega evento", church, List.of());
    var workshop =
        childOf(
            conference, EventId.generate(), "Taller", conference.getStart(), conference.getEnd());
    when(eventRepository.findById(conference.getId())).thenReturn(Optional.of(conference));
    when(eventRepository.findByParentOrderByStart(conference)).thenReturn(List.of(workshop));

    handler.handle(new DeleteEventCommand(conference.getId(), EventChangeOption.THIS_EVENT));

    assertThat(conference.getDeletedTime()).isNotNull();
    assertThat(workshop.getDeletedTime()).isNotNull();
    verify(deleteAttendanceForEventsCommandHandler)
        .handle(
            new DeleteAttendanceForEventsCommand(List.of(conference.getId(), workshop.getId())));
  }

  @Test
  void deletingASubEventLeavesItsContainer() {
    var church = series.getOrganization();
    var conference = singleEvent(EventId.generate(), "Mega evento", church, List.of());
    var workshop =
        childOf(
            conference, EventId.generate(), "Taller", conference.getStart(), conference.getEnd());
    when(eventRepository.findById(workshop.getId())).thenReturn(Optional.of(workshop));

    handler.handle(new DeleteEventCommand(workshop.getId(), EventChangeOption.THIS_EVENT));

    assertThat(workshop.getDeletedTime()).isNotNull();
    assertThat(conference.getDeletedTime()).isNull();
  }
}
