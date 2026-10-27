package org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities;

import java.util.List;

public interface GetPersonAccountabilitiesQueryHandler {
  List<PersonAccountabilityResult> handle(GetPersonAccountabilitiesQuery query);
}
