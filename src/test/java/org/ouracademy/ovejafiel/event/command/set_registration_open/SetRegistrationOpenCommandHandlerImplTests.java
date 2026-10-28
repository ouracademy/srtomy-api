package org.ouracademy.ovejafiel.event.command.set_registration_open;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.registrationSetup;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.command.WaitlistPromotion;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationSetup;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class SetRegistrationOpenCommandHandlerImplTests {

  EventRegistrationSetupRepository setups = mock(EventRegistrationSetupRepository.class);
  WaitlistPromotion promotion = mock(WaitlistPromotion.class);
  SetRegistrationOpenCommandHandlerImpl handler =
      new SetRegistrationOpenCommandHandlerImpl(setups, promotion);

  EventId id = EventId.generate();
  Organization group = organization(PartyId.generate(), "Jóvenes", Organization.Type.GROUP);
  // The fixture setup starts open.
  EventRegistrationSetup setup =
      registrationSetup(singleEvent(id, "Retiro", group, List.of()), false, null);

  @Test
  void closesTheRegistrationAndKeepsTheRest() {
    when(setups.lockByEventId(id)).thenReturn(Optional.of(setup));

    handler.handle(new SetRegistrationOpenCommand(id, false));

    assertThat(setup.isOpen()).isFalse();
    verify(promotion, never()).promote(any());
  }

  @Test
  void reopeningLetsWhoWaitsTakeTheSeatsThatAreFree() {
    setup.setOpen(false);
    when(setups.lockByEventId(id)).thenReturn(Optional.of(setup));

    handler.handle(new SetRegistrationOpenCommand(id, true));

    assertThat(setup.isOpen()).isTrue();
    verify(promotion).promote(setup);
  }

  @Test
  void anEventThatTakesNoSignUpsIsNotFound() {
    when(setups.lockByEventId(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new SetRegistrationOpenCommand(id, false)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
