package org.ouracademy.ovejafiel.person.query.find_people_by_phone;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindPeopleByPhoneQueryHandlerImpl implements FindPeopleByPhoneQueryHandler {

  static final int MIN_DIGITS = 8;

  private final PersonRepository personRepository;

  @Override
  @Transactional(readOnly = true)
  public FindPeopleByPhoneResult handle(FindPeopleByPhoneQuery query) {
    var digits = digitsOf(query.phone());
    if (digits.length() < MIN_DIGITS) {
      return new FindPeopleByPhoneResult(List.of());
    }
    return new FindPeopleByPhoneResult(
        personRepository.findByPhoneInChurch(digits, query.churchId().value()).stream()
            .map(
                row ->
                    new FindPeopleByPhoneResult.Match(
                        new PartyId(row.getId()), row.getName(), row.getWhatsappConsent()))
            .toList());
  }

  static String digitsOf(String phone) {
    return phone == null ? "" : phone.replaceAll("[^0-9]", "");
  }
}
