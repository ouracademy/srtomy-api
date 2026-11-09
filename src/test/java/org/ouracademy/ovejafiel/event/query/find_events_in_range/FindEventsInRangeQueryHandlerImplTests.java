package org.ouracademy.ovejafiel.event.query.find_events_in_range;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.FindEventResult;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class FindEventsInRangeQueryHandlerImplTests {

  @Mock EventRepository eventRepository;

  @Test
  void returnsTheEventsOfTheGivenNodesWithoutResolvingTheSubtreeAgain() {
    var handler = new FindEventsInRangeQueryHandlerImpl(eventRepository);
    var organizationId = PartyId.generate();
    var groupId = PartyId.generate();
    var church = organization(organizationId, "Iglesia", Organization.Type.CHURCH);
    var start = Instant.parse("2026-01-01T00:00:00Z");
    var end = Instant.parse("2026-12-31T00:00:00Z");
    var event = singleEvent(EventId.generate(), "Culto", church, List.of());
    when(eventRepository.searchEventsOfOrgIdsAndBetweenDates(
            List.of(organizationId, groupId),
            start.atZone(ZoneOffset.UTC),
            end.atZone(ZoneOffset.UTC)))
        .thenReturn(List.of(event));

    var result =
        handler.handle(new FindEventsInRangeQuery(List.of(organizationId, groupId), start, end));

    assertThat(result).extracting(FindEventResult::id).containsExactly(event.getId());
  }
}
