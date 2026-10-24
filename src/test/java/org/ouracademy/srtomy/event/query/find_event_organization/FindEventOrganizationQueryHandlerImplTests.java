package org.ouracademy.srtomy.event.query.find_event_organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.event.domain.Event;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class FindEventOrganizationQueryHandlerImplTests {

  @Mock EventRepository eventRepository;
  @InjectMocks FindEventOrganizationQueryHandlerImpl handler;

  @Test
  void returnsTheNodeTheEventBelongsTo() {
    var id = EventId.generate();
    var organizationId = PartyId.generate();
    var organization = mock(Organization.class);
    when(organization.getId()).thenReturn(organizationId);
    var event = mock(Event.class);
    when(event.getOrganization()).thenReturn(organization);
    when(eventRepository.findById(id)).thenReturn(Optional.of(event));

    assertThat(handler.handle(new FindEventOrganizationQuery(id)))
        .contains(new EventOrganizationResult(organizationId));
  }

  @Test
  void isEmptyForAnUnknownEvent() {
    var id = EventId.generate();
    when(eventRepository.findById(id)).thenReturn(Optional.empty());

    assertThat(handler.handle(new FindEventOrganizationQuery(id))).isEmpty();
  }
}
