package org.ouracademy.ovejafiel.event.query.find_event_by_id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.childOf;
import static org.ouracademy.ovejafiel.event.EventFixtures.eventSeries;
import static org.ouracademy.ovejafiel.event.EventFixtures.occurrenceOf;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;
import static org.ouracademy.ovejafiel.person.PersonFixtures.person;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.Recurrence;
import org.ouracademy.ovejafiel.event.domain.RegistrationMode;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class FindEventByIdQueryHandlerImplTests {

  @Mock EventRepository eventRepository;

  FindEventByIdQueryHandlerImpl handler;

  Organization church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);

  @BeforeEach
  void setUp() {
    handler = new FindEventByIdQueryHandlerImpl(eventRepository);
  }

  @Test
  void returnsTheSingleEventWithItsOrganizationAndSpeakers() {
    var id = EventId.generate();
    var speakerId = PartyId.generate();
    var event = singleEvent(id, "Culto", church, List.of(person(speakerId, "Jose", "Mena")));
    when(eventRepository.findById(id)).thenReturn(Optional.of(event));

    var result = handler.handle(new FindEventByIdQuery(id)).orElseThrow();

    assertThat(result.id()).isEqualTo(id);
    assertThat(result.title()).isEqualTo("Culto");
    assertThat(result.start()).isEqualTo(event.getStart());
    assertThat(result.end()).isEqualTo(event.getEnd());
    assertThat(result.address()).isEqualTo("Av. Siempre Viva 123");
    assertThat(result.description()).isEqualTo("Culto dominical");
    assertThat(result.isACourse()).isFalse();
    assertThat(result.organization().id()).isEqualTo(church.getId());
    assertThat(result.speakers())
        .singleElement()
        .satisfies(s -> assertThat(s.id()).isEqualTo(speakerId));
    assertThat(result.recurrence()).isNull();
  }

  @Test
  void includesTheRecurrenceOfAnOccurrenceOfASeries() {
    var series = eventSeries(church);
    var occurrence = occurrenceOf(series, EventId.generate());
    when(eventRepository.findById(occurrence.getId())).thenReturn(Optional.of(occurrence));

    var result = handler.handle(new FindEventByIdQuery(occurrence.getId())).orElseThrow();

    assertThat(result.recurrence().seriesId()).isEqualTo(series.getId());
    assertThat(result.recurrence().frequency()).isEqualTo(Recurrence.Frequency.WEEKLY);
    assertThat(result.recurrence().daysOfWeek()).containsExactly(1, 3);
    assertThat(result.recurrence().endType()).isEqualTo(Recurrence.EndType.UNTIL);
    assertThat(result.recurrence().until()).isEqualTo(LocalDate.of(2026, 1, 14));
    assertThat(result.recurrence().timeZone()).isEqualTo("UTC");
  }

  @Test
  void isEmptyWhenTheEventDoesNotExist() {
    var id = EventId.generate();
    when(eventRepository.findById(id)).thenReturn(Optional.empty());

    assertThat(handler.handle(new FindEventByIdQuery(id))).isEmpty();
  }

  @Test
  void aContainerListsItsSubEventsInOrder() {
    var conference = singleEvent(EventId.generate(), "Mega evento", church, List.of());
    var workshop =
        childOf(
            conference, EventId.generate(), "Taller", conference.getStart(), conference.getEnd());
    workshop.setRegistrationMode(RegistrationMode.OWN);
    when(eventRepository.findById(conference.getId())).thenReturn(Optional.of(conference));
    when(eventRepository.findByParentOrderByStart(conference)).thenReturn(List.of(workshop));

    var result = handler.handle(new FindEventByIdQuery(conference.getId())).orElseThrow();

    assertThat(result.parentEventId()).isNull();
    assertThat(result.children())
        .singleElement()
        .satisfies(
            child -> {
              assertThat(child.id()).isEqualTo(workshop.getId());
              assertThat(child.title()).isEqualTo("Taller");
              assertThat(child.registrationMode()).isEqualTo(RegistrationMode.OWN);
            });
  }

  @Test
  void aSubEventSaysWhichContainerItBelongsTo() {
    var conference = singleEvent(EventId.generate(), "Mega evento", church, List.of());
    var workshop =
        childOf(
            conference, EventId.generate(), "Taller", conference.getStart(), conference.getEnd());
    when(eventRepository.findById(workshop.getId())).thenReturn(Optional.of(workshop));

    var result = handler.handle(new FindEventByIdQuery(workshop.getId())).orElseThrow();

    assertThat(result.parentEventId()).isEqualTo(conference.getId());
    assertThat(result.parentTitle()).isEqualTo("Mega evento");
    assertThat(result.registrationMode()).isEqualTo(RegistrationMode.INHERIT);
    assertThat(result.children()).isEmpty();
  }
}
