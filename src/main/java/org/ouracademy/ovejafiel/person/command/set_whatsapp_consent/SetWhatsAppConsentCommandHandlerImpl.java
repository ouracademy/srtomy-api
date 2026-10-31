package org.ouracademy.ovejafiel.person.command.set_whatsapp_consent;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SetWhatsAppConsentCommandHandlerImpl implements SetWhatsAppConsentCommandHandler {

  private final PersonRepository personRepository;

  @Override
  @Transactional
  public SetWhatsAppConsentResult handle(SetWhatsAppConsentCommand command) {
    var changed = 0;
    for (var person : personRepository.findAllById(command.personIds())) {
      if (person.isWhatsappConsent() != command.consent()) {
        person.setWhatsappConsent(command.consent());
        changed++;
      }
    }
    return new SetWhatsAppConsentResult(changed);
  }
}
