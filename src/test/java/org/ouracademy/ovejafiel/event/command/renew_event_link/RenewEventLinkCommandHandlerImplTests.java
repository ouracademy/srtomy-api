package org.ouracademy.ovejafiel.event.command.renew_event_link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class RenewEventLinkCommandHandlerImplTests {

  EventRepository repository = mock(EventRepository.class);
  RenewEventLinkCommandHandlerImpl handler = new RenewEventLinkCommandHandlerImpl(repository);

  EventId id = EventId.generate();

  @Test
  void givesTheEventANewAddress() {
    var event =
        singleEvent(
            id,
            "Retiro",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            List.of());
    var old = event.getShareSlug();
    when(repository.findById(id)).thenReturn(Optional.of(event));

    var result = handler.handle(new RenewEventLinkCommand(id));

    assertThat(result.slug()).isEqualTo(event.getShareSlug()).isNotEqualTo(old);
  }

  @Test
  void anUnknownEventIsNotFound() {
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new RenewEventLinkCommand(id)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
