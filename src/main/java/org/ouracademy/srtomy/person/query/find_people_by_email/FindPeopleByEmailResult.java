package org.ouracademy.srtomy.person.query.find_people_by_email;

import java.util.List;
import org.ouracademy.srtomy.party.domain.PartyId;

public record FindPeopleByEmailResult(List<PartyId> personIds) {}
