package org.ouracademy.srtomy.person.query;

import java.util.List;
import org.ouracademy.srtomy.party.domain.PartyId;

/** Bulk variant of {@link FindPersonByIdQuery}: resolves many person ids in a single call. */
public record FindPeopleByIdsQuery(List<PartyId> ids) {}
