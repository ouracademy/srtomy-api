package org.ouracademy.ovejafiel.person.command.set_whatsapp_consent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;

class SetWhatsAppConsentCommandHandlerImplTests {

  PersonRepository repository = mock(PersonRepository.class);
  SetWhatsAppConsentCommandHandlerImpl handler =
      new SetWhatsAppConsentCommandHandlerImpl(repository);

  @Test
  void turnsConsentOffAndCountsOnlyWhoChanged() {
    var accepts = new Person();
    var alreadyOff = new Person();
    alreadyOff.setWhatsappConsent(false);
    var ids = List.of(PartyId.generate(), PartyId.generate());
    when(repository.findAllById(ids)).thenReturn(List.of(accepts, alreadyOff));

    var result = handler.handle(new SetWhatsAppConsentCommand(ids, false));

    assertThat(result.changed()).isEqualTo(1);
    assertThat(accepts.isWhatsappConsent()).isFalse();
    assertThat(alreadyOff.isWhatsappConsent()).isFalse();
  }

  @Test
  void canTurnItBackOn() {
    var person = new Person();
    person.setWhatsappConsent(false);
    var ids = List.of(PartyId.generate());
    when(repository.findAllById(ids)).thenReturn(List.of(person));

    assertThat(handler.handle(new SetWhatsAppConsentCommand(ids, true)).changed()).isEqualTo(1);
    assertThat(person.isWhatsappConsent()).isTrue();
  }
}
