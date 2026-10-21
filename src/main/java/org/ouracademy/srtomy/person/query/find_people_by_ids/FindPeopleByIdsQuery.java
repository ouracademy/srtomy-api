package org.ouracademy.srtomy.person.query.find_people_by_ids;

import java.util.List;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.query.find_person_by_id.FindPersonByIdQuery;

/** Bulk variant of {@link FindPersonByIdQuery}: resolves many person ids in a single call. */
public record FindPeopleByIdsQuery(List<PartyId> ids) {}
