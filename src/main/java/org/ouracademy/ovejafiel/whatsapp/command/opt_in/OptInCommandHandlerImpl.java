package org.ouracademy.ovejafiel.whatsapp.command.opt_in;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.person.command.set_whatsapp_consent.SetWhatsAppConsentCommand;
import org.ouracademy.ovejafiel.person.command.set_whatsapp_consent.SetWhatsAppConsentCommandHandler;
import org.ouracademy.ovejafiel.person.query.find_people_by_phone.FindPeopleByPhoneQuery;
import org.ouracademy.ovejafiel.person.query.find_people_by_phone.FindPeopleByPhoneQueryHandler;
import org.ouracademy.ovejafiel.person.query.find_people_by_phone.FindPeopleByPhoneResult;
import org.ouracademy.ovejafiel.whatsapp.repository.WhatsAppConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class OptInCommandHandlerImpl implements OptInCommandHandler {

  private final WhatsAppConnectionRepository connections;
  private final FindPeopleByPhoneQueryHandler findPeopleByPhone;
  private final SetWhatsAppConsentCommandHandler setConsent;

  @Override
  @Transactional
  public OptInResult handle(OptInCommand command) {
    var connection = connections.findByPhoneNumberId(command.phoneNumberId());
    if (connection.isEmpty()) {
      return new OptInResult(0);
    }
    var people =
        findPeopleByPhone
            .handle(
                new FindPeopleByPhoneQuery(command.from(), connection.get().getOrganizationId()))
            .people();
    setConsent.handle(
        new SetWhatsAppConsentCommand(
            people.stream().map(FindPeopleByPhoneResult.Match::id).toList(), true));
    return new OptInResult(people.size());
  }
}
