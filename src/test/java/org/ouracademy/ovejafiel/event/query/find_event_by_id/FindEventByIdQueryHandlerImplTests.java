package org.ouracademy.ovejafiel.event.query.find_event_by_id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
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
}
