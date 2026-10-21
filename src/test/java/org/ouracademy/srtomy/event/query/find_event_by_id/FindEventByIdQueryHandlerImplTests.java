package org.ouracademy.srtomy.event.query.find_event_by_id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.event.EventFixtures.occurrenceOf;
import static org.ouracademy.srtomy.event.EventFixtures.recurrentEvent;
import static org.ouracademy.srtomy.event.EventFixtures.singleEvent;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;
import static org.ouracademy.srtomy.person.PersonFixtures.person;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.repository.SingleEventRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class FindEventByIdQueryHandlerImplTests {

  @Mock SingleEventRepository singleEventRepository;

  FindEventByIdQueryHandlerImpl handler;

  Organization church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);

  @BeforeEach
  void setUp() {
    handler = new FindEventByIdQueryHandlerImpl(singleEventRepository);
  }

  @Test
  void returnsTheSingleEventWithItsOrganizationAndSpeakers() {
    var id = EventId.generate();
    var speakerId = PartyId.generate();
    var event = singleEvent(id, "Culto", church, List.of(person(speakerId, "Jose", "Mena")));
    when(singleEventRepository.findById(id)).thenReturn(Optional.of(event));

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
    assertThat(result.recurrentEvent()).isNull();
  }

  @Test
  void includesTheRecurrenceOfAnExpandedOccurrence() {
    var recurrent = recurrentEvent(EventId.generate(), church);
    var occurrence = occurrenceOf(recurrent, EventId.generate());
    when(singleEventRepository.findById(occurrence.getId())).thenReturn(Optional.of(occurrence));

    var result = handler.handle(new FindEventByIdQuery(occurrence.getId())).orElseThrow();

    assertThat(result.recurrentEvent().id()).isEqualTo(recurrent.getId());
    assertThat(result.recurrentEvent().daysOfWeek()).containsExactly(1, 3);
    assertThat(result.recurrentEvent().endRecur()).isEqualTo(LocalDate.of(2026, 1, 14));
    assertThat(result.recurrentEvent().organization().id()).isEqualTo(church.getId());
  }

  @Test
  void isEmptyWhenTheEventDoesNotExist() {
    var id = EventId.generate();
    when(singleEventRepository.findById(id)).thenReturn(Optional.empty());

    assertThat(handler.handle(new FindEventByIdQuery(id))).isEmpty();
  }
}
