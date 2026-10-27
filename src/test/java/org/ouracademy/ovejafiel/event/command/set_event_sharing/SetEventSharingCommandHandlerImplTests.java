package org.ouracademy.ovejafiel.event.command.set_event_sharing;

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
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;
import org.ouracademy.ovejafiel.event.domain.LinkRole;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class SetEventSharingCommandHandlerImplTests {

  EventRepository repository = mock(EventRepository.class);
  SetEventSharingCommandHandlerImpl handler = new SetEventSharingCommandHandlerImpl(repository);

  EventId id = EventId.generate();

  @Test
  void changesWhoCanOpenTheEventAndWhatTheLinkAllows() {
    var event =
        singleEvent(
            id,
            "Curso",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            List.of());
    event.setIsACourse(true);
    when(repository.findById(id)).thenReturn(Optional.of(event));

    handler.handle(new SetEventSharingCommand(id, GeneralAccess.CHURCH, LinkRole.REGISTRANT));

    assertThat(event.getGeneralAccess()).isEqualTo(GeneralAccess.CHURCH);
    assertThat(event.getLinkRole()).isEqualTo(LinkRole.REGISTRANT);
  }

  @Test
  void aPlainEventCannotOpenItsRegistration() {
    var event =
        singleEvent(
            id,
            "Retiro",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            List.of());
    when(repository.findById(id)).thenReturn(Optional.of(event));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new SetEventSharingCommand(id, GeneralAccess.CHURCH, LinkRole.REGISTRANT)))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void anUnknownEventIsNotFound() {
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                handler.handle(
                    new SetEventSharingCommand(id, GeneralAccess.CHURCH, LinkRole.VIEWER)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
