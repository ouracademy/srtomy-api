package org.ouracademy.srtomy.event.command.unshare_event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.event.EventFixtures.singleEvent;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.EventShare;
import org.ouracademy.srtomy.event.domain.EventShareId;
import org.ouracademy.srtomy.event.domain.ShareRole;
import org.ouracademy.srtomy.event.repository.EventShareRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

class UnshareEventCommandHandlerImplTests {

  EventShareRepository repository = mock(EventShareRepository.class);
  UnshareEventCommandHandlerImpl handler = new UnshareEventCommandHandlerImpl(repository);

  EventId eventId = EventId.generate();
  EventShareId shareId = EventShareId.generate();

  EventShare shareOf(EventId owner) {
    var event =
        singleEvent(
            owner,
            "Retiro",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            List.of());
    return new EventShare(event, "ana@x.org", ShareRole.VIEWER, null);
  }

  @Test
  void removesTheShareOfThatEvent() {
    var share = shareOf(eventId);
    when(repository.findById(shareId)).thenReturn(Optional.of(share));

    handler.handle(new UnshareEventCommand(eventId, shareId));

    verify(repository).delete(share);
  }

  @Test
  void aShareOfAnotherEventIsNotFoundAndNotRemoved() {
    var share = shareOf(EventId.generate());
    when(repository.findById(shareId)).thenReturn(Optional.of(share));

    assertThatThrownBy(() -> handler.handle(new UnshareEventCommand(eventId, shareId)))
        .isInstanceOf(NoSuchElementException.class);
    verify(repository, never()).delete(share);
  }

  @Test
  void anUnknownShareIsNotFound() {
    when(repository.findById(shareId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new UnshareEventCommand(eventId, shareId)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
