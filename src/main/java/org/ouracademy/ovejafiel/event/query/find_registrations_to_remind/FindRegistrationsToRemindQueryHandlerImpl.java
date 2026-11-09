package org.ouracademy.ovejafiel.event.query.find_registrations_to_remind;

import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.query.find_registrations_to_remind.FindRegistrationsToRemindResult.Item;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindRegistrationsToRemindQueryHandlerImpl implements FindRegistrationsToRemindQueryHandler {

  private final EventRegistrationRepository registrations;

  @Override
  @Transactional(readOnly = true)
  public FindRegistrationsToRemindResult handle(FindRegistrationsToRemindQuery query) {
    var found =
        registrations.findToRemind(
            query.from().atZone(ZoneOffset.UTC), query.to().atZone(ZoneOffset.UTC));
    return new FindRegistrationsToRemindResult(
        found.stream()
            .map(
                r ->
                    new Item(
                        r.getId().value(),
                        r.getFirstName(),
                        r.getPhoneNumber(),
                        r.getEvent().getTitle(),
                        r.getEvent().getStart(),
                        r.getEvent().getAddress(),
                        r.getEvent().getOrganization().getId()))
            .toList());
  }
}
