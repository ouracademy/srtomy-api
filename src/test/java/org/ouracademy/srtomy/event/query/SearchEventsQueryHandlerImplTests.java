package org.ouracademy.srtomy.event.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.event.EventFixtures.singleEvent;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class SearchEventsQueryHandlerImplTests {

  @Mock EventRepository eventRepository;

  @Test
  void returnsTheOrganizationsEventsBetweenTheDates() {
    var handler = new SearchEventsQueryHandlerImpl(eventRepository);
    var organizationId = PartyId.generate();
    var church = organization(organizationId, "Iglesia", Organization.Type.CHURCH);
    var start = ZonedDateTime.parse("2026-01-01T00:00:00Z[UTC]");
    var end = ZonedDateTime.parse("2026-12-31T00:00:00Z[UTC]");
    var first = singleEvent(EventId.generate(), "Culto", church, List.of());
    var second = singleEvent(EventId.generate(), "Oracion", church, List.of());
    when(eventRepository.searchEventsOfOrgIdAndBetweenDates(organizationId, start, end))
        .thenReturn(List.of(first, second));

    var result = handler.handle(new SearchEventsQuery(organizationId, start, end));

    assertThat(result)
        .extracting(FindEventResult::id, FindEventResult::title)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(first.getId(), "Culto"),
            org.assertj.core.groups.Tuple.tuple(second.getId(), "Oracion"));
  }
}
